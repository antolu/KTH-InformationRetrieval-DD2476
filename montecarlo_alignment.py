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

f, (ax1, ax2) = plt.subplots(1,2, sharey=True)

ax1.set_title("Goodness vs N")
for i in range(4) :
    j = i
    if i > 2 :
        j += 1
    ax1.plot(np.log10(N), goodness[i], label="MonteCarlo " + str(j))
ax1.set_xlabel(r"$\log_{10}N$")
ax1.set_ylabel(r"$Goodness (euclidean)$")
ax1.legend()

ax2.set_title("Time consumption vs N")
for i in range(4) :
    j = i + 1
    if i > 2 :
        j += 1
    ax2.plot(np.log10(N), times[i], label="MonteCarlo " + str(j))
ax2.set_xlabel(r"$\log_{10}N$")
ax2.set_ylabel(r"Computation time (s)")
ax2.legend()

plt.show()
plt.close()