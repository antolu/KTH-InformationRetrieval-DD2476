import numpy as np
import matplotlib.pyplot as plt

goodness = []
times = []
N = []

readN = False
for i in range(4) :
    with open("data/goodnessMC" + str(i)) as f:
        dataContent = f.readlines()
    # you may also want to remove whitespace characters like `\n` at the end of each line

    data = []
    for string in dataContent :
        split = string.split(",")
        if not readN :
            N.append(int(split[0]))
        data.append(float(split[1]))

    readN = True

    with open("data/timesMC" + str(i)) as f:
        timeContent = f.readlines()

    
    time = []
    for string in timeContent :
        split = string.split(",")

        time.append(float(split[1]))

    goodness.append(np.asarray(data))
    times.append(np.asarray(time))

N = np.asarray(N)

f, (ax1, ax2) = plt.subplots(1,2, sharex=True)

ax1.set_title("Goodness vs N")
for i in range(4) :
    j = i + 1
    if i > 1 :
        j += 1
    ax1.semilogx(N, goodness[i], label="MonteCarlo " + str(j))
ax1.set_xlabel(r"$N$")
ax1.set_ylabel(r"$Goodness (euclidean)$")
ax1.legend()
# ax1.set_xlim(5, 9)
# ax1.set_ylim(0, 1e-3)

ax2.set_title("Time consumption vs N")
for i in range(4) :
    j = i + 1
    if i > 1 :
        j += 1
    ax2.semilogx(N, times[i], label="MonteCarlo " + str(j))
ax2.set_xlabel(r"$N$")
ax2.set_ylabel(r"Computation time (s)")
ax2.legend()
# ax2.set_xlim(5, 9)
# ax2.set_ylim(0, 35)

f.set_size_inches(20, 10)

plt.savefig("data/montecarlo_plots.eps")
plt.show()
plt.close()