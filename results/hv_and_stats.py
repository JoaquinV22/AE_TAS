#!/usr/bin/env python3
"""
Calcula el hipervolumen por ejecucion y las estadisticas de tiempo de ejecucion a partir de los CSV,
luego ejecuta pruebas de hipotesis y escribe los resultados en archivos CSV

A partir de:
  - tas_nsgaii_config_runs.csv
  - tas_final_eval_runs.csv

Genera:
  analisis_tuning/
    - tas_nsgaii_config_hv_runs.csv
    - tas_nsgaii_config_stats.csv
    - tas_nsgaii_config_kruskal.csv
    - tas_nsgaii_config_pairwise_vs_chosen.csv

  analisis_eval/
    - tas_final_eval_hv_runs.csv
    - tas_final_eval_stats.csv
    - tas_final_eval_mannwhitney.csv
"""

from __future__ import annotations

from pathlib import Path
import numpy as np
import pandas as pd

TUNING_IN = Path("tas_nsgaii_config_runs.csv")
FINAL_IN  = Path("tas_final_eval_runs.csv")

OUT_TUNING_DIR = Path("analisis_tuning")
OUT_EVAL_DIR   = Path("analisis_eval")

OUT_TUNING_DIR.mkdir(parents=True, exist_ok=True)
OUT_EVAL_DIR.mkdir(parents=True, exist_ok=True)

# columnas

# CSV config params
T_INST = "instance"
T_POP  = "populationSize"
T_PC   = "crossoverProb"
T_PM   = "mutationProb"
T_RUN  = "run"
T_SOL  = "solutionIndex"
T_F1   = "f1_makespan"
T_F2   = "f2_dissatisfaction"
T_TIME = "timeMillis"

# CSV eval final
F_INST = "instance"
F_ALG  = "algorithm"
F_POP  = "populationSize"
F_PC   = "crossoverProb"
F_PM   = "mutationProb"
F_RUN  = "run"
F_SOL  = "solutionIndex"
F_F1   = "f1_makespan"
F_F2   = "f2_dissatisfaction"
F_TIME = "timeMillis"

# configuracion elegida (N=120, c_p=1.0 y m_p=0.1) para compararla por pares con las demas configuraciones posibles
CHOSEN_CFG = {
    "populationSize": 120,
    "crossoverProb": 1.0,
    "mutationProb": 0.10,
}

# Punto de referencia epsilon: "ligeramente peor que el peor"
REF_EPS = 1.0

# Pareto / HV
def nondominated_2d(points: list[tuple[float, float]]) -> list[tuple[float, float]]:
    """devuelve subconjunto no dominado para minimizacion en 2D"""
    pts = sorted(points, key=lambda x: (x[0], x[1]))  # ordenar por f1 y despues f2 ascendente
    nd: list[tuple[float, float]] = []
    best_f2 = float("inf")
    for f1, f2 in pts:
        if f2 < best_f2:
            nd.append((f1, f2))
            best_f2 = f2
    return nd

def hypervolume_2d_min(nd_points: list[tuple[float, float]], ref: tuple[float, float]) -> float:
    """
    hipervolumen 2d para minimizacion con punto de referencia ref=(r1,r2).
    """
    r1, r2 = ref
    pts = [(f1, f2) for (f1, f2) in nd_points if f1 <= r1 and f2 <= r2]
    if not pts:
        return 0.0

    pts = sorted(pts, key=lambda x: x[0])  # f1 creciente
    hv = 0.0
    cur_y = r2
    for f1, f2 in pts:
        if f2 < cur_y:
            hv += (r1 - f1) * (cur_y - f2)
            cur_y = f2
    return float(hv)

def compute_reference_point(df: pd.DataFrame, f1_col: str, f2_col: str, eps: float = 1.0) -> tuple[float, float]:
    """(max(f1)+eps, max(f2)+eps) so it dominates all observed points in the dataframe."""
    return (float(df[f1_col].max() + eps), float(df[f2_col].max() + eps))

def hv_per_run(
    df: pd.DataFrame,
    group_cols: list[str],
    f1_col: str,
    f2_col: str,
    time_col: str,
    ref: tuple[float, float],
) -> pd.DataFrame:
    """
    calcular HV por ejecucion. group_cols tiene que identificar una sola ejecucion.
    añade:
      - hv: hipervolumen
      - timeMillis: tiempo de ejecucion (maximo de la columna de tiempo en el grupo)
      - n_points: numero de filas de soluciones en el grupo
      - n_nd: numero de puntos no dominados en el grupo
    """
    rows = []
    for key, g in df.groupby(group_cols, dropna=False):
        pts = list(zip(g[f1_col].astype(float), g[f2_col].astype(float)))
        nd = nondominated_2d(pts)
        hv = hypervolume_2d_min(nd, ref)

        # tiempo de ejecucion por ejecucion: el tiempo se repite por fila, tomar el maximo
        t = float(g[time_col].max()) if time_col in g.columns else float("nan")

        if not isinstance(key, tuple):
            key = (key,)
        rows.append(list(key) + [hv, t, len(pts), len(nd)])

    return pd.DataFrame(rows, columns=group_cols + ["hv", "timeMillis", "n_points", "n_nd"])

def summarize(df_hv: pd.DataFrame, group_cols: list[str]) -> pd.DataFrame:
    """media/desviacion estandar de los tiempos de ejecucion para hv y timeMillis"""
    return (
        df_hv.groupby(group_cols)
        .agg(
            hv_mean=("hv", "mean"),
            hv_std=("hv", "std"),
            time_mean=("timeMillis", "mean"),
            time_std=("timeMillis", "std"),
            n_runs=("hv", "count"),
        )
        .reset_index()
    )

# pruebas estadisticas
def require_scipy() -> None:
    try:
        import scipy
    except Exception as e:
        raise RuntimeError("scipy is required for hypothesis tests. Install with: pip install scipy") from e

def mann_whitney_u(x: np.ndarray, y: np.ndarray) -> tuple[float, float]:
    from scipy.stats import mannwhitneyu
    res = mannwhitneyu(x, y, alternative="two-sided")
    return float(res.statistic), float(res.pvalue)

def kruskal_wallis(groups: list[np.ndarray]) -> tuple[float, float]:
    from scipy.stats import kruskal
    res = kruskal(*groups)
    return float(res.statistic), float(res.pvalue)

# main
def main() -> None:
    require_scipy()

    # tuning
    df_t = pd.read_csv(TUNING_IN)

    df_t[T_F1] = pd.to_numeric(df_t[T_F1], errors="coerce")
    df_t[T_F2] = pd.to_numeric(df_t[T_F2], errors="coerce")
    df_t[T_TIME] = pd.to_numeric(df_t[T_TIME], errors="coerce")
    if df_t[[T_F1, T_F2]].isna().any().any():
        raise ValueError("Found NaNs in tuning objectives after conversion. Check input CSV.")

    ref_t = compute_reference_point(df_t, T_F1, T_F2, eps=REF_EPS)

    hv_t = hv_per_run(
        df_t,
        group_cols=[T_INST, T_POP, T_PC, T_PM, T_RUN],
        f1_col=T_F1,
        f2_col=T_F2,
        time_col=T_TIME,
        ref=ref_t,
    )
    (OUT_TUNING_DIR / "tas_nsgaii_config_hv_runs.csv").write_text(
        hv_t.to_csv(index=False), encoding="utf-8"
    )

    stats_t = summarize(hv_t, [T_INST, T_POP, T_PC, T_PM])
    (OUT_TUNING_DIR / "tas_nsgaii_config_stats.csv").write_text(
        stats_t.to_csv(index=False), encoding="utf-8"
    )

    # Kruskal-Wallis por instancia en todas las configuraciones
    kw_rows = []
    for inst_name, g in hv_t.groupby(T_INST):
        samples = [gg["hv"].values for _, gg in g.groupby([T_POP, T_PC, T_PM])]
        if len(samples) < 2:
            continue

        all_vals = np.concatenate(samples)
        # SciPy genera un error si todos los números son identicos
        if np.unique(all_vals).size <= 1:
            kw_rows.append([inst_name, 0.0, 1.0, len(samples), "all_identical"])
        else:
            H, p = kruskal_wallis(samples)
            kw_rows.append([inst_name, H, p, len(samples), "ok"])

    df_kw = pd.DataFrame(kw_rows, columns=[T_INST, "kruskal_H", "p_value", "n_configs", "status"])
    (OUT_TUNING_DIR / "tas_nsgaii_config_kruskal.csv").write_text(
        df_kw.to_csv(index=False), encoding="utf-8"
    )

    # Mann-Whitney por pares vs la configuracion elegida (por instancia)
    if CHOSEN_CFG is not None:
        pair_rows = []

        chosen_mask = (
            (hv_t[T_POP] == CHOSEN_CFG["populationSize"])
            & (hv_t[T_PC] == CHOSEN_CFG["crossoverProb"])
            & (hv_t[T_PM] == CHOSEN_CFG["mutationProb"])
        )

        for inst_name, g in hv_t.groupby(T_INST):
            g_ch = g[chosen_mask.loc[g.index]]
            if len(g_ch) == 0:
                continue

            for cfg, gg in g.groupby([T_POP, T_PC, T_PM]):
                # saltear chosen vs ella misma
                if (
                    cfg[0] == CHOSEN_CFG["populationSize"]
                    and cfg[1] == CHOSEN_CFG["crossoverProb"]
                    and cfg[2] == CHOSEN_CFG["mutationProb"]
                ):
                    continue

                U, p = mann_whitney_u(g_ch["hv"].values, gg["hv"].values)
                pair_rows.append([inst_name, *cfg, U, p, len(g_ch), len(gg)])

        df_pair = pd.DataFrame(
            pair_rows,
            columns=[T_INST, T_POP, T_PC, T_PM, "U_stat", "p_value", "n_chosen", "n_other"],
        )
        (OUT_TUNING_DIR / "tas_nsgaii_config_pairwise_vs_chosen.csv").write_text(
            df_pair.to_csv(index=False), encoding="utf-8"
        )

    # evaluacion final
    df_f = pd.read_csv(FINAL_IN)

    df_f[F_F1] = pd.to_numeric(df_f[F_F1], errors="coerce")
    df_f[F_F2] = pd.to_numeric(df_f[F_F2], errors="coerce")
    df_f[F_TIME] = pd.to_numeric(df_f[F_TIME], errors="coerce")
    if df_f[[F_F1, F_F2]].isna().any().any():
        raise ValueError("Found NaNs in final objectives after conversion. Check input CSV.")

    ref_f = compute_reference_point(df_f, F_F1, F_F2, eps=REF_EPS)

    hv_f = hv_per_run(
        df_f,
        group_cols=[F_INST, F_ALG, F_RUN],
        f1_col=F_F1,
        f2_col=F_F2,
        time_col=F_TIME,
        ref=ref_f,
    )
    (OUT_EVAL_DIR / "tas_final_eval_hv_runs.csv").write_text(
        hv_f.to_csv(index=False), encoding="utf-8"
    )

    stats_f = summarize(hv_f, [F_INST, F_ALG])
    (OUT_EVAL_DIR / "tas_final_eval_stats.csv").write_text(
        stats_f.to_csv(index=False), encoding="utf-8"
    )

    # Mann–Whitney por instancia: NSGA-II vs Greedy (HV + runtime)
    mw_rows = []
    for inst_name, g in hv_f.groupby(F_INST):
        algs_upper = {str(a).upper(): a for a in g[F_ALG].unique()}
        if "NSGAII" in algs_upper and "GREEDY" in algs_upper:
            ns = g[g[F_ALG] == algs_upper["NSGAII"]]
            gr = g[g[F_ALG] == algs_upper["GREEDY"]]
            if len(ns) == 0 or len(gr) == 0:
                continue

            U_hv, p_hv = mann_whitney_u(ns["hv"].values, gr["hv"].values)
            U_t, p_t = mann_whitney_u(ns["timeMillis"].values, gr["timeMillis"].values)

            mw_rows.append([inst_name, "hv", U_hv, p_hv, len(ns), len(gr)])
            mw_rows.append([inst_name, "timeMillis", U_t, p_t, len(ns), len(gr)])

    df_mw = pd.DataFrame(
        mw_rows,
        columns=[F_INST, "metric", "U_stat", "p_value", "n_nsga", "n_greedy"],
    )
    (OUT_EVAL_DIR / "tas_final_eval_mannwhitney.csv").write_text(
        df_mw.to_csv(index=False), encoding="utf-8"
    )

    print("Done. Wrote:")
    print(f"  {OUT_TUNING_DIR}/tas_nsgaii_config_hv_runs.csv")
    print(f"  {OUT_TUNING_DIR}/tas_nsgaii_config_stats.csv")
    print(f"  {OUT_TUNING_DIR}/tas_nsgaii_config_kruskal.csv")
    if CHOSEN_CFG is not None:
        print(f"  {OUT_TUNING_DIR}/tas_nsgaii_config_pairwise_vs_chosen.csv")
    print(f"  {OUT_EVAL_DIR}/tas_final_eval_hv_runs.csv")
    print(f"  {OUT_EVAL_DIR}/tas_final_eval_stats.csv")
    print(f"  {OUT_EVAL_DIR}/tas_final_eval_mannwhitney.csv")

if __name__ == "__main__":
    main()

