import pandas as pd

STATS_CSV = "tas_final_eval_stats.csv"

def main():
    # leer el CSV de stats de la evaluacion final
    stats = pd.read_csv(STATS_CSV)

    # orden de las instancias
    instance_order = [
        "instancia_pequena.json",
        "instancia_mediana.json",
        "instancia_grande.json",
    ]
    stats["instance"] = pd.Categorical(
        stats["instance"],
        categories=instance_order,
        ordered=True
    )
    stats = stats.sort_values(["instance", "algorithm"])

    # elegimos las columnas que queremos en la tabla
    tabla = stats[[
        "instance",
        "algorithm",
        "hv_mean",
        "hv_std",
        "time_mean",
        "time_std",
        "n_runs"
    ]]

    # 1) CSV
    tabla.to_csv("tabla_final_eval.csv", index=False)
    print("Guardado tabla_final_eval.csv")

    # 2) Markdown
    print("\nTabla de evaluación final (Markdown):\n")
    print(tabla.to_markdown(index=False))


if __name__ == "__main__":
    main()

