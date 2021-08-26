package core;

public class svm_model {

	public svm_parameter param; // parameter
	public int nr_class; // number of classes, = 2 in regression/one class svm
	public int l; // total #SV
	public svm_node[][] SV; // SVs (SV[l])
	public double[][] sv_coef; // coefficients for SVs in decision functions (sv_coef[k-1][l])
	public double[] rho; // constants in decision functions (rho[k*(k-1)/2])

	// for classification only
	public int[] label; // label of each class (label[k])
	public int[] nSV; // number of SVs for each class (nSV[k])
	// nSV[0] + nSV[1] + ... + nSV[k-1] = l
};

class svm_node {
	public int index;
	public double value;
}