import pandas as pd

STATS_CSV = "tas_nsgaii_config_stats.csv"

# leer el CSV
stats = pd.read_csv(STATS_CSV)

# filtrar por instancia mediana
med = stats[stats["instance"] == "instancia_mediana.json"].copy()

# elegir columnas y ordenarlas
med = med[[
    "populationSize",
    "crossoverProb",
    "mutationProb",
    "hv_mean",
    "hv_std",
    "time_mean",
    "time_std"
]]

# ordenar por hv_mean descendente (las mejores primero)
med = med.sort_values("hv_mean", ascending=False)

# 1) guardar una version reducida como CSV
med.to_csv("tabla_mediana_configs.csv", index=False)
print("Guardado tabla_mediana_configs.csv")

# 2) imprimir en formato Markdown (para pegar en el informe)
print("\nTabla en formato Markdown:\n")
print(med.to_markdown(index=False))

# 3) table LaTeX
# with open("tabla_mediana_configs.tex", "w") as f:
#     f.write(med.to_latex(index=False, float_format="%.3f"))
# print("Guardado tabla_mediana_configs.tex (formato LaTeX).")

