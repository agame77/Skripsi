package core;

import com.virtenio.misc.StringUtils;

public class svm {

	//
	// construct and solve various formulations
	//
	public static final int LIBSVM_VERSION = 323;

	// method untuk mengembalikan type svm dari model data
	public static int svm_get_svm_type(svm_model model) {
		return model.param.svm_type;
	}

	// method untuk mengembalikan jumlah class dari model data
	public static int svm_get_nr_class(svm_model model) {
		return model.nr_class;
	}

	// method untuk mengembalikan label dari model data
	public static void svm_get_labels(svm_model model, int[] label) {
		if (model.label != null) {
			for (int i = 0; i < model.nr_class; i++) {
				label[i] = model.label[i];
			}
		}
	}

	public static double svm_predict_values(svm_model model, svm_node[] x, double[] dec_values) {
		int i;
		int nr_class = model.nr_class;
		int l = model.l;

		double[] kvalue = new double[l];
		for (i = 0; i < l; i++) {
			kvalue[i] = Kernel.k_function(x, model.SV[i], model.param);
		}

		//start untuk menentukan mulainya penghitungan untuk masing" kelas
		int[] start = new int[nr_class];
		start[0] = 0;
		for (i = 1; i < nr_class; i++) {
			start[i] = start[i - 1] + model.nSV[i - 1];
		}

		int[] vote = new int[nr_class];
		for (i = 0; i < nr_class; i++) {
			vote[i] = 0;
		}

		int p = 0;
		for (i = 0; i < nr_class; i++) {
			for (int j = i + 1; j < nr_class; j++) {
				double sum = 0;
				int si = start[i];
				int sj = start[j];
				int ci = model.nSV[i];
				int cj = model.nSV[j];

				int k;
				double[] coef1 = model.sv_coef[j - 1];
				double[] coef2 = model.sv_coef[i];
				for (k = 0; k < ci; k++) {
					sum += coef1[si + k] * kvalue[si + k];
				}
				for (k = 0; k < cj; k++) {
					sum += coef2[sj + k] * kvalue[sj + k];
				}
				
				sum -= model.rho[p];
				dec_values[p] = sum;
				if (dec_values[p] > 0) {
					++vote[i];
				} else {
					++vote[j];
				}
				p++;
			}
		}

		int vote_max_idx = 0;
		for (i = 1; i < nr_class; i++) {
			if (vote[i] > vote[vote_max_idx]) {
				vote_max_idx = i;
			}
		}
		return model.label[vote_max_idx];
	}

	public static double svm_predict(svm_model model, svm_node[] x) {
		int nr_class = model.nr_class;
		double[] dec_values;
		dec_values = new double[nr_class * (nr_class - 1) / 2];
		double pred_result = svm_predict_values(model, x, dec_values);
		return pred_result;
	}

	static final String svm_type_table[] = { "c_svc" };

	static final String kernel_type_table[] = { "linear" };

	// method untuk membaca header pada kelas model_data
	private static boolean read_model_header(svm_model model) {
		svm_parameter param = new svm_parameter();
		model.param = param;

		try {
			for (int x = 0; x < model_data.header.length; x++) {
				String cmd = model_data.header[x];
				String arg = cmd.substring(cmd.indexOf(' ') + 1);
				if (cmd.startsWith("svm_type")) {
					int i;
					for (i = 0; i < svm_type_table.length; i++) {
						if (arg.indexOf(svm_type_table[i]) != -1) {
							param.svm_type = i;
							break;
						}
					}
					if (i == svm_type_table.length) {
						System.err.print("unknown svm type.\n");
						return false;
					}
				} else if (cmd.startsWith("kernel_type")) {
					int i;
					for (i = 0; i < kernel_type_table.length; i++) {
						if (arg.indexOf(kernel_type_table[i]) != -1) {
							param.kernel_type = i;
							break;
						}
					}
					if (i == kernel_type_table.length) {
						System.err.print("unknown kernel function.\n");
						return false;
					}
				} else if (cmd.startsWith("nr_class")) {
					model.nr_class = Integer.parseInt(arg);
				} else if (cmd.startsWith("total_sv")) {
					model.l = Integer.parseInt(arg);
				} else if (cmd.startsWith("rho")) {
					int n = model.nr_class * (model.nr_class - 1) / 2;
					model.rho = new double[n];
					String value = StringUtils.split(cmd, " ")[1];
					for (int i = 0; i < n; i++) {
						model.rho[i] = Double.parseDouble(value);
					}
				} else if (cmd.startsWith("label")) {
					int n = model.nr_class;
					model.label = new int[n];
					String[] value = StringUtils.split(cmd, " ");
					for (int i = 0; i < n; i++) {
						model.label[i] = Integer.parseInt(value[i + 1]);
					}
				} else if (cmd.startsWith("nr_sv")) {
					int n = model.nr_class;
					model.nSV = new int[n];
					String[] value = StringUtils.split(cmd, " ");
					for (int i = 0; i < n; i++) {
						model.nSV[i] = Integer.parseInt(value[i + 1]);
					}
				} else if (cmd.startsWith("SV")) {
					break;
				}
			}

		} catch (Exception e) {
			return false;
		}
		return true;

	}

	// method untuk membaca label/coefisien dan value dari model_data
	public static svm_model svm_load_model() {

		// read parameters
		svm_model model = new svm_model();

		model_data md = new model_data();
		model.label = null;
		model.nSV = null;

		if (read_model_header(model) == false) {
			System.err.print("ERROR: failed to read model\n");
			return null;
		}

		// read sv_coef and SV
		int l = model.l + 1;
		model.SV = new svm_node[l][];
		// mengambil isi coefisien atau label dari satu baris model value
		model.sv_coef = model_data.sv_coef;
		int position = 0;
		for (int i = 0; i < model_data.sv_string.length; i++) {
			// split setiap index dan value dari model value
			String[] idx_values = StringUtils.split(model_data.sv_string[i], " ");
			int n = idx_values.length;
			model.SV[position] = new svm_node[n];
			for (int j = 0; j < idx_values.length; j++) {
				// split index dan value dalam 1 baris model value
				String[] idx_value = StringUtils.split(idx_values[j], ":");
				model.SV[position][j] = new svm_node();
				model.SV[position][j].index = Integer.parseInt(idx_value[0]);
				model.SV[position][j].value = Double.parseDouble(idx_value[1]);
			}
			position++;
		}
		return model;
	}
}
class svm_parameter implements Cloneable {

	/* svm_type */
	public static final int C_SVC = 0;

	/* kernel_type */
	public static final int LINEAR = 0;

	public int svm_type;
	public int kernel_type;

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}

class Kernel {

	public static double dot(svm_node[] x, svm_node[] y) {
		double sum = 0;
		int xlen = x.length;
		int ylen = y.length;
		int i = 0;
		int j = 0;
		while (i < xlen && j < ylen) {
			if (x[i].index == y[j].index) {
				sum += x[i++].value * y[j++].value;
			} else {
				if (x[i].index > y[j].index) {
					++j;
				} else {
					++i;
				}
			}
		}
		return sum;
	}

	static double k_function(svm_node[] x, svm_node[] y, svm_parameter param) {
		switch (param.kernel_type) {
		case svm_parameter.LINEAR:
			return dot(x, y);
		default:
			return 0; // java
		}
	}
}



