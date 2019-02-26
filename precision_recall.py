import numpy as np
import matplotlib.pyplot as plt

precision_ranked = np.zeros(5)
recall_ranked = np.zeros(5)

precision_intersection = np.zeros(5)
recall_intersection = np.zeros(5)

precision_ranked[0] = 4.0/10.0
precision_ranked[1] = 5.0/20.0
precision_ranked[2] = 11.0/30.0
precision_ranked[3] = 19.0/40.0
precision_ranked[4] = 26.0/50.0

recall_ranked[0] = 4.0/100.0
recall_ranked[1] = 5.0/100.0
recall_ranked[2] = 11.0/100.0
recall_ranked[3] = 19.0/100.0
recall_ranked[4] = 26.0/100.0

precision_intersection[0] = 7.0/10.0
precision_intersection[1] = 14.0/20.0
precision_intersection[2] = 16.0/30.0
precision_intersection[3] = 16.0/40.0
precision_intersection[4] = 16.0/50.0

recall_intersection[0] = 7.0/100.0
recall_intersection[1] = 14.0/100.0
recall_intersection[2] = 16.0/100.0
recall_intersection[3] = 16.0/100.0
recall_intersection[4] = 16.0/100.0

plt.plot(precision_ranked, recall_ranked)
plt.xlabel("precision")
plt.ylabel("recall")
plt.show()
plt.close()

plt.plot(precision_intersection, recall_intersection)
plt.xlabel("precision")
plt.ylabel("recall")
plt.show()
plt.close()