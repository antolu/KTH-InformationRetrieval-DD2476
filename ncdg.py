import numpy as np
import math as m

prerelevance = []

prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)

prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)

prerelevance.append(0)
prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)

prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(1)

prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(1)
prerelevance.append(1)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)
prerelevance.append(0)

precdg = (2.0**prerelevance[0] - 1) / m.log2(2)

for i in range(1, len(prerelevance)) :
    precdg += (2**prerelevance[i] - 1) / m.log2(i+2)

prerelevance.sort(reverse=True)

preidcg = (2.0**prerelevance[0] - 1) / m.log2(2)

for i in range(1, len(prerelevance)) :
    preidcg += (2**prerelevance[i] - 1) / m.log2(i+2)

precdg /= preidcg

postrelevance = []

postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(1)
# postrelevance.append(3) Mathematics.f
postrelevance.append(1)
postrelevance.append(2)
postrelevance.append(2)

postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(2)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(2)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(0)

postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(1)
postrelevance.append(0)

postrelevance.append(2)
postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(2)
postrelevance.append(0)
postrelevance.append(0)

postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(1)
postrelevance.append(0)
postrelevance.append(2)
postrelevance.append(0)
postrelevance.append(0)
postrelevance.append(0)

postdcg = (2.0**postrelevance[0] - 1) / m.log2(2)

for i in range(1, len(postrelevance)) :
    postdcg += (2**postrelevance[i] - 1) / m.log2(i+2)

postrelevance.sort(reverse=True)

postidcg = (2.0**postrelevance[0] - 1) / m.log2(2)

for i in range(1, len(postrelevance)) :
    postidcg += (2**postrelevance[i] - 1) / m.log2(i+2)

postdcg /= postidcg

print("Before relevance feedback: ", precdg)
print("After relevance feedback: ", postdcg)