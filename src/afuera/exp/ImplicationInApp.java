package afuera.exp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import afuera.core.Signaler;
import afuera.flow.config.FileConfig;
import afuera.preprocess.apiparsing.SelfAPIList;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;


public class ImplicationInApp {
	public static String path_android_platform = FileConfig.SERVER_ANDROID_PLATFORM;
	public static void main(String args[]) throws IOException {
		List<String> imp_apis;
		imp_apis = read(FileConfig.IMPLICATION_api);
		analyze(new File(args[0]), imp_apis);
	}
	
	public static List<String> read(String filePath) throws IOException{
		List<String> apis = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		String line = null;
		while((line = br.readLine())!=null) {
			apis.add(line.split(",")[0]);
		}
		br.close();
		return apis;
	}
	
	public static double analyze(File app, List<String> imp_apis) {
		double ratio = 0d;
//		List<String> ue_used = new ArrayList<String>();
//		List<String> all_used= new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		G.reset();
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);;
		List<String> processPaths = new ArrayList<String>();		
		Options.v().set_src_prec(Options.src_prec_apk);
		//Options.v().set_android_jars(FileConfig.SERVER_ANDROID_PLATFORM);
		Options.v().set_android_jars(path_android_platform);
		Options.v().set_android_api_version(19);
		processPaths.add(app.getAbsolutePath());
		Options.v().set_process_dir(processPaths);
		Options.v().set_output_format(Options.output_format_none);
	    //Options.v().set_validate(true); // Validate Jimple bodies in each transofrmation pack
		Scene.v().loadNecessaryClasses();
//		SelfAPIList apiList = new SelfAPIList("res/GeneratedAPIList.txt");
//		List<SootMethod> entryPoints = apiList.getEntryPoints();
//		Scene.v().setEntryPoints(entryPoints);
//		List<Signaler> signalers = new ArrayList<Signaler>();
		PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
				CallGraph cg = Scene.v().getCallGraph();
				final PatchingChain<Unit> units = b.getUnits();		
				//important to use snapshotIterator here
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					u.apply(new AbstractStmtSwitch() {

						public void caseInvokeStmt(InvokeStmt stmt) {
							if(stmt.containsInvokeExpr()) {
								InvokeExpr invkExpr = stmt.getInvokeExpr();
								String methodSig = invkExpr.getMethod().getSignature();
								if(imp_apis.contains(methodSig)) {
									//all_used.add(methodSig);
									sb.append(b.toString());
									sb.append("\n");
								
									//System.out.println(invkExpr.getMethod().getSignature());
									//TODO: Is this Body b invoked by a Component callback, or itself is one?
									//analyzeStackTrace(new ArrayList<StackFrame>(), cg);
									//TODO: Is this Body b handled before invoked by a Component callback, or it handled this api by itself?
								}
//								if(ue_apis.contains(methodSig)) {
//									ue_used.add(methodSig);
//								}
							}
						}

					});
				}
			}
		}));
		PackManager.v().runPacks();
		G.reset();
		try {
			if(sb.toString().length()>=10)
				write("script/imp/"+app.getName()+".txt",sb);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ratio;
	}
	
	public static void write(String filePath, StringBuilder sb) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filePath)));
		bw.write(sb.toString());
		bw.newLine();
		bw.close();
	}
}
