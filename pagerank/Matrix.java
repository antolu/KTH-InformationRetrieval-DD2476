package pagerank;

public class Matrix {

    public double[][] mtx;
    public int m;
    public int n;

    public Matrix(int m, int n) {
        mtx = new double[m][n];
        this.m = m;
        this.n = n;
    }
    
    public static Matrix multiply(Matrix a, Matrix b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Matrix prod = new Matrix(m, n);
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                for( int k = 0; k < b.m; k++){
                    prod.mtx[i][j] += a.mtx[i][k] * b.mtx[k][j];
                }                
            }
        }
        return prod;
    }

    public static Matrix add(Matrix a, Matrix b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Matrix ret = new Matrix(m, n);
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                ret.mtx[i][j] = a.mtx[i][j] + b.mtx[i][j];
            }
        }

        return ret;
    }

    public static Matrix subtract(Matrix a, Matrix b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Matrix ret = new Matrix(m, n);
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                ret.mtx[i][j] = a.mtx[i][j] - b.mtx[i][j];
            }
        }

        return ret;
    }

    public static double distance(Matrix a, Matrix b) throws IllegalArgumentException {
        
        int m = a.m;
        int n = b.n;

        if (a.m != b.m || a.n != b.n) {
            throw new IllegalArgumentException("Incompatible matrix dimensions: " + a.m + " x " + a.n + " " + b.m + "x" + b.n);
        }

        Matrix ret = new Matrix(m, n);

        double alignment = 0.0;
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                alignment += Math.pow(a.mtx[i][j] - b.mtx[i][j], 2);
            }
        }

        alignment = Math.sqrt(alignment);

        return alignment;
    }

    public static double sum(Matrix a) {
        double sum = 0.0;

        for (int i = 0; i < a.m; i++){
            for (int j = 0; j < a.n; j++){
                sum += a.mtx[i][j];
            }
        }

        return sum;
    }

    public static void scalarMult(Matrix a, double c) throws IllegalArgumentException {
        
        int m = a.m;
        int n = a.n;
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                a.mtx[i][j] *= c;
            }
        }
    }

    public static void normalize(Matrix a) {
        
        int m = a.m;
        int n = a.n;
        
        for (int i = 0; i < m; i++){
            double norm = 0.0;
            for (int j = 0; j < n; j++){
                norm += a.mtx[i][j];
            }

            for (int j = 0; j < n; j++){
                a.mtx[i][j] /= norm;
            }
        }
    }

    public static Matrix fillMatrix(int m, int n, double c) throws IllegalArgumentException {
        
        Matrix matrix = new Matrix(m, n);
        
        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                matrix.mtx[i][j] = c;
            }
        }

        return matrix;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(); 

        for (int i = 0; i < m; i++){
            for (int j = 0; j < n; j++){
                sb.append(mtx[i][j] + " ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}