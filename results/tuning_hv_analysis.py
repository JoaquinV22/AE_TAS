import math
import os
import pandas as pd

# rutas y directorios
INPUT_CSV = "tas_nsgaii_config_runs.csv"
OUTPUT_DIR = "analisis_tuning"

os.makedirs(OUTPUT_DIR, exist_ok=True)

HV_RUNS_CSV   = os.path.join(OUTPUT_DIR, "tas_nsgaii_config_hv_runs.csv")
STATS_CSV     = os.path.join(OUTPUT_DIR, "tas_nsgaii_config_stats.csv")

# leer el CSV
df = pd.read_csv(INPUT_CSV)

# renombrar columnas por comodidad
df = df.rename(columns={
    "f1_makespan": "f1",
    "f2_dissatisfaction": "f2"
})

# elegir el punto de referencia R
# R tiene que ser peor que todas las soluciones:
f1_max = df["f1"].max()
f2_max = df["f2"].max()

R1 = f1_max + 1.0
R2 = f2_max + 1.0
REF_POINT = (R1, R2)

print("Punto de referencia R =", REF_POINT)

# funciones para HV 2D (minimizacion)

def pareto_nd_min(points):
    """
    Devuelve el conjunto no dominado (aprox) de una lista de puntos (f1,f2) en minimizacion.
    Version simple: ordena por f1 ascendente y se queda con los mejores en f2.
    """
    pts = sorted(points)  # ordena por f1 asc, y luego por f2 asc
    nd = []
    best_f2 = math.inf
    for f1, f2 in pts:
        if f2 < best_f2:
            nd.append((f1, f2))
            best_f2 = f2
    return nd

def hypervolume_2d_min(points, ref):
    """
    Hipervolumen para problema 2D de minimizacion, respecto a un punto de referencia ref=(R1,R2).
    Implementa la logica de los rectangulos tipo W0, W1, W2...
    """
    R1, R2 = ref
    if not points:
        return 0.0

    # nos quedamos con el frente no dominado
    nd = pareto_nd_min(points)

    # ordenar por f1 ascendente
    nd = sorted(nd, key=lambda x: x[0])

    hv = 0.0
    prev_f2 = R2

    for f1, f2 in nd:
        # si el punto esta fuera del rectangulo de referencia, lo ignoramos
        if f1 > R1 or f2 > prev_f2:
            continue

        width = R1 - f1
        height = prev_f2 - f2

        if width < 0 or height < 0:
            continue

        hv += width * height
        prev_f2 = f2

    return hv

# calcular HV por ejecución (instance, pop, pc, pm, run)

group_cols_run = ["instance", "populationSize", "crossoverProb", "mutationProb", "run"]
hv_records = []

for group_key, group_df in df.groupby(group_cols_run):
    instance, pop, cp, mp, run = group_key

    # todos los puntos (f1,f2) de ESA ejecucion
    points = list(zip(group_df["f1"], group_df["f2"]))

    hv = hypervolume_2d_min(points, REF_POINT)

    # tomamos un timeMillis (todos en el grupo son iguales)
    time_millis = group_df["timeMillis"].iloc[0]

    hv_records.append({
        "instance": instance,
        "populationSize": pop,
        "crossoverProb": cp,
        "mutationProb": mp,
        "run": run,
        "hv": hv,
        "timeMillis": time_millis
    })

hv_df = pd.DataFrame(hv_records)
hv_df.to_csv(HV_RUNS_CSV, index=False)
print(f"Guardado {HV_RUNS_CSV} con HV por run.")

# resumen por configuracion (instance, pop, pc, pm)

group_cols_cfg = ["instance", "populationSize", "crossoverProb", "mutationProb"]

config_stats = hv_df.groupby(group_cols_cfg).agg(
    hv_mean=("hv", "mean"),
    hv_std=("hv", "std"),
    time_mean=("timeMillis", "mean"),
    time_std=("timeMillis", "std"),
    n_runs=("hv", "count")
).reset_index()

config_stats.to_csv(STATS_CSV, index=False)
print(f"Guardado {STATS_CSV} con medias y desviaciones por configuración.")

print("\nPrimeras filas del resumen:")
print(config_stats.head())

