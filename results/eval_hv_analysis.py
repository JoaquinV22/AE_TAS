import math
import pandas as pd
import os

OUT_DIR = "analisis_eval"
os.makedirs(OUT_DIR, exist_ok=True)

INPUT_CSV = "tas_final_eval_runs.csv"

# leer CSV de evaluacion final
df = pd.read_csv(INPUT_CSV)

# renombrar por comodidad
df = df.rename(columns={
    "f1_makespan": "f1",
    "f2_dissatisfaction": "f2"
})

# Elegir punto de referencia R para HV (nuevo para evaluacion)
f1_max = df["f1"].max()
f2_max = df["f2"].max()

R1 = f1_max + 1.0
R2 = f2_max + 1.0
REF_POINT = (R1, R2)

print("Punto de referencia R (evaluación) =", REF_POINT)

# funciones de Pareto y HV (las mismas que antes)

def pareto_nd_min(points):
    """Devuelve el conjunto no dominado (minimizacion 2D)."""
    pts = sorted(points)  # ordena por f1 asc, luego f2 asc
    nd = []
    best_f2 = math.inf
    for f1, f2 in pts:
        if f2 < best_f2:
            nd.append((f1, f2))
            best_f2 = f2
    return nd

def hypervolume_2d_min(points, ref):
    """Hipervolumen 2D (minimizacion) respecto a ref=(R1,R2)."""
    R1, R2 = ref
    if not points:
        return 0.0

    nd = pareto_nd_min(points)
    nd = sorted(nd, key=lambda x: x[0])  # por f1

    hv = 0.0
    prev_f2 = R2

    for f1, f2 in nd:
        if f1 > R1 or f2 > prev_f2:
            continue
        width = R1 - f1
        height = prev_f2 - f2
        if width < 0 or height < 0:
            continue
        hv += width * height
        prev_f2 = f2

    return hv

# HV por ejecucion: (instance, algorithm, run)

group_cols_run = ["instance", "algorithm", "run"]
hv_records = []

for (instance, algo, run), group_df in df.groupby(group_cols_run):
    points = list(zip(group_df["f1"], group_df["f2"]))

    hv = hypervolume_2d_min(points, REF_POINT)

    # timeMillis: como siempre, todas las filas del run comparten el mismo tiempo
    time_millis = group_df["timeMillis"].iloc[0]

    hv_records.append({
        "instance": instance,
        "algorithm": algo,
        "run": run,
        "hv": hv,
        "timeMillis": time_millis
    })

hv_df = pd.DataFrame(hv_records)
hv_df.to_csv(os.path.join(OUT_DIR, "tas_final_eval_hv_runs.csv"), index=False)
print("Guardado tas_final_eval_hv_runs.csv (HV por run).")

# resumen por instancia y algoritmo

group_cols_alg = ["instance", "algorithm"]

stats = hv_df.groupby(group_cols_alg).agg(
    hv_mean=("hv", "mean"),
    hv_std=("hv", "std"),
    time_mean=("timeMillis", "mean"),
    time_std=("timeMillis", "std"),
    n_runs=("hv", "count")
).reset_index()

stats.to_csv(os.path.join(OUT_DIR, "tas_final_eval_stats.csv"), index=False)
print("Guardado tas_final_eval_stats.csv (resumen por instancia y algoritmo).")

print("\nResumen:")
print(stats)

