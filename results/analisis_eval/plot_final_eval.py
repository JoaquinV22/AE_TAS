import pandas as pd
import matplotlib.pyplot as plt

STATS_CSV = "tas_final_eval_stats.csv"

def main():
    # leer el CSV de estadisticas de la evaluacion final
    stats = pd.read_csv(STATS_CSV)

    # columnas del CSV:
    # instance, algorithm, hv_mean, hv_std, time_mean, time_std, n_runs

    # instancias en orden
    instance_order = [
        "instancia_pequena.json",
        "instancia_mediana.json",
        "instancia_grande.json",
    ]

    # pivot para tener una tabla instancia * algoritmo
    pivot_hv = stats.pivot(index="instance", columns="algorithm", values="hv_mean")
    pivot_time = stats.pivot(index="instance", columns="algorithm", values="time_mean")

    # weordenar filas segun instance_order
    pivot_hv = pivot_hv.reindex(instance_order)
    pivot_time = pivot_time.reindex(instance_order)

    # nombres para el eje X
    nice_index = ["Pequeña", "Mediana", "Grande"]
    pivot_hv.index = nice_index
    pivot_time.index = nice_index

    # grafico de barras de hipervolumen medio
    plt.figure()
    ax1 = pivot_hv.plot(kind="bar")
    ax1.set_ylabel("Hipervolumen medio (HV_mean)")
    ax1.set_xlabel("Instancia")
    ax1.set_title("Hipervolumen medio por instancia y algoritmo")
    plt.xticks(rotation=0)  # nombres de instancias horizontales
    plt.tight_layout()
    plt.savefig("final_eval_hv_mean_bar.png", dpi=300)
    plt.close()

    # grafico de barras de tiempo medio (ms)
    plt.figure()
    ax2 = pivot_time.plot(kind="bar")
    ax2.set_ylabel("Tiempo medio (ms)")
    ax2.set_xlabel("Instancia")
    ax2.set_title("Tiempo medio de ejecución por instancia y algoritmo")
    plt.xticks(rotation=0)
    plt.tight_layout()
    plt.savefig("final_eval_time_mean_bar.png", dpi=300)
    plt.close()

    print("Gráficas guardadas como:")
    print("  final_eval_hv_mean_bar.png")
    print("  final_eval_time_mean_bar.png")

if __name__ == "__main__":
    main()

