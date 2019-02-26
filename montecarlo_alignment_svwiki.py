import numpy as np
import matplotlib.pyplot as plt

goodness = []
times = []
N = []

with open("data/goodnessSvwiki") as f:
    dataContent = f.readlines()

data = []
for string in dataContent :
    split = string.split(",")
    N.append(int(split[0]))
    data.append(float(split[1]))


with open("data/timesSvwiki") as g:
    timeContent = g.readlines()

time = []
for string in timeContent :
    split = string.split(",")

    time.append(float(split[1]))

goodness = np.asarray(data)
times = np.asarray(time)

N = np.asarray(N)

f, (ax1, ax2) = plt.subplots(1,2, sharex=True)

ax1.set_title("Goodness vs N")

ax1.semilogx(N, goodness, label="MonteCarlo 4 Svwiki")
ax1.set_xlabel(r"$N$")
ax1.set_ylabel(r"$Goodness (euclidean)$")
ax1.legend()
# ax1.set_xlim(5, 9)
# ax1.set_ylim(0, 1e-3)

ax2.set_title("Time consumption vs N")

ax2.semilogx(N, times, label="MonteCarlo 4 Svwiki")
ax2.set_xlabel(r"$N$")
ax2.set_ylabel(r"Computation time (s)")
ax2.legend()
# ax2.set_xlim(5, 9)
# ax2.set_ylim(0, 35)

f.set_size_inches(20, 10)

plt.savefig("data/montecarlo_plots_svwiki.eps")
plt.show()
plt.close()