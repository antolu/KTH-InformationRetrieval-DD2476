import numpy as np
import matplotlib.pyplot as plt

for i in range(4) :
    with open("data/goodnessMC" + i) as f:
        content = f.readlines()
    # you may also want to remove whitespace characters like `\n` at the end of each line
    content = [x.strip() for x in content] 