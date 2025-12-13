import pandas as pd
import matplotlib.pyplot as plt

STATS_CSV = "tas_nsgaii_config_stats.csv"

# leer el CSV de configuracion
stats = pd.read_csv(STATS_CSV)

# nombres de columnas esperados:
# instance, populationSize, crossoverProb, mutationProb,
# hv_mean, hv_std, time_mean, time_std, n_runs

# filtrar una instancia: mediana (la que distingue mejor en HV)
inst_med = stats[stats["instance"] == "instancia_mediana.json"].copy()

print("Configs en instancia_mediana.json:", len(inst_med))

# Scatter: hv_mean vs time_mean (trade-off calidad/tiempo)
plt.figure()
plt.scatter(inst_med["time_mean"], inst_med["hv_mean"])

# Etiquetas de ejes y título
plt.xlabel("Mean Time (ms)")
plt.ylabel("Mean Hypervolume (HV_mean)")
plt.title("NSGA-II: trade-off HV vs time (instancia mediana)")

# algunos puntos interesantes (pop 120 y 200)
configs_to_label = [
    (120, 1.0, 0.10),
    (200, 1.0, 0.10),
]

for _, row in inst_med.iterrows():
    key = (int(row["populationSize"]), row["crossoverProb"], row["mutationProb"])
    if key in configs_to_label:
        label = f"N={key[0]}, pc={key[1]}, pm={key[2]}"
        plt.annotate(
            label,
            (row["time_mean"], row["hv_mean"]),
            textcoords="offset points",
            xytext=(5, 5),
            fontsize=8
        )

plt.tight_layout()
plt.savefig("tuning_tradeoff_hv_vs_time_mediana.png", dpi=300)
print("Guardado tuning_tradeoff_hv_vs_time_mediana.png")

# HV_mean vs poblacion para pc=1.0, pm=0.10

pc_fix = 1.0
pm_fix = 0.10

subset = inst_med[
    (inst_med["crossoverProb"] == pc_fix) &
    (inst_med["mutationProb"] == pm_fix)
].copy()

subset = subset.sort_values("populationSize")

print("\nConfigs con pc=1.0, pm=0.10 en instancia_mediana:")
print(subset[["populationSize", "hv_mean", "time_mean"]])

plt.figure()
plt.plot(subset["populationSize"], subset["hv_mean"], marker="o")
plt.xlabel("Population size (N)")
plt.ylabel("Mean Hypervolume (HV_mean)")
plt.title(f"Mean HV vs population (pc={pc_fix}, pm={pm_fix}) - instancia mediana")
plt.xticks(subset["populationSize"])

plt.tight_layout()
plt.savefig("tuning_hv_vs_population_mediana_pc1_pm01.png", dpi=300)
print("Guardado tuning_hv_vs_population_mediana_pc1_pm01.png")

