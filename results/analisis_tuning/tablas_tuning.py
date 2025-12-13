import pandas as pd

STATS_CSV = "tas_nsgaii_config_stats.csv"

def main():
    # leer el CSV de stats de tuning
    stats = pd.read_csv(STATS_CSV)

    # nos quedamos solo con la instancia mediana
    med = stats[stats["instance"] == "instancia_mediana.json"].copy()

    # nos quedamos con las columnas que queremos ver en la tabla
    med = med[[
        "populationSize",
        "crossoverProb",
        "mutationProb",
        "hv_mean",
        "hv_std",
        "time_mean",
        "time_std"
    ]]

    # ordenamos por HV medio (mejores primero)
    med = med.sort_values("hv_mean", ascending=False)

    # 1) guardar version completa como CSV
    med.to_csv("tabla_tuning_mediana_configs.csv", index=False)
    print("Guardado tabla_tuning_mediana_configs.csv")

    # 2) imprimir en formato Markdown
    print("\nTabla de tuning (instancia mediana) en formato Markdown:\n")
    print(med.to_markdown(index=False))

if __name__ == "__main__":
    main()

