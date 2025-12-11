import pandas as pd

stats = pd.read_csv("tas_nsgaii_config_stats.csv")

# Instancia pequeña
small = stats[stats["instance"] == "instancia_pequena.json"] \
            .sort_values("hv_mean", ascending=False)
print("TOP configs - instancia_pequena:")
print(small.head(10))

# Instancia mediana
medium = stats[stats["instance"] == "instancia_mediana.json"] \
             .sort_values("hv_mean", ascending=False)
print("\nTOP configs - instancia_mediana:")
print(medium.head(10))

