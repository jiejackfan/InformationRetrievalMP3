package edu.virginia.cs.evaluator;

import edu.virginia.cs.index.ResultDoc;
import edu.virginia.cs.index.Searcher;
import edu.virginia.cs.index.similarities.*;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;


public class Evaluate {
	/**
	 * Format for judgements.txt is:
	 * 
	 * line 0: <query 1 text> line 1: <space-delimited list of relevant URLs>
	 * line 2: <query 2 text> line 3: <space-delimited list of relevant URLs>
	 * ...
	 * Please keep all these constants!
	 */

	Searcher _searcher = null;

//	public static void setSimilarity(Searcher searcher, String method) {
//        if(method == null)
//            return;
//        else if(method.equals("--ok"))
//            searcher.setSimilarity(new BM25Similarity());
//        else if(method.equals("--tfidf"))
//            searcher.setSimilarity(new DefaultSimilarity());
//        else
//        {
//            System.out.println("[Error]Unknown retrieval function specified!");
//            printUsage();
//            System.exit(1);
//        }
//    }

	public static void setSimilarity(Searcher searcher, String method) {
		if(method == null)
			return;
		else if(method.equals("--dp"))
			searcher.setSimilarity(new DirichletPrior());
		else if(method.equals("--jm"))
			searcher.setSimilarity(new JelinekMercer());
		else if(method.equals("--ok"))
			searcher.setSimilarity(new OkapiBM25());
		else if(method.equals("--pl"))
			searcher.setSimilarity(new PivotedLength());
		else if(method.equals("--tfidf"))
			searcher.setSimilarity(new TFIDFDotProduct());
		else if(method.equals("--bdp"))
			searcher.setSimilarity(new BooleanDotProduct());
		else
		{
			System.out.println("[Error]Unknown retrieval function specified!");
			printUsage();
			System.exit(1);
		}
	}

//    public static void printUsage()
//    {
//        System.out.println("To specify a ranking function, make your last argument one of the following:");
//        System.out.println("\t--ok\tOkapi BM25");
//        System.out.println("\t--tfidf\tTFIDF Dot Product");
//    }
	public static void printUsage() {
		System.out.println("To specify a ranking function, make your last argument one of the following:");
		System.out.println("\t--dp\tDirichlet Prior");
		System.out.println("\t--jm\tJelinek-Mercer");
		System.out.println("\t--ok\tOkapi BM25");
		System.out.println("\t--pl\tPivoted Length Normalization");
		System.out.println("\t--tfidf\tTFIDF Dot Product");
		System.out.println("\t--bdp\tBoolean Dot Product");
	}


	//
	ArrayList<Double> okMAP = new ArrayList<Double>();
	ArrayList<Double> tfidfMAP = new ArrayList<Double>();
	ArrayList<Double> okPK = new ArrayList<Double>();
	ArrayList<Double> tfidfPK = new ArrayList<Double>();
	ArrayList<Double> okMRR = new ArrayList<Double>();
	ArrayList<Double> tfidfMRR = new ArrayList<Double>();
	ArrayList<Double> okNDCG = new ArrayList<Double>();
	ArrayList<Double> tfidfNDCG = new ArrayList<Double>();

	public void pVal() {
		WilcoxonSignedRankTest wilcoxon = new WilcoxonSignedRankTest();
		TTest ttest = new TTest();
		// paired t test for MAP
		//double[] dblArray = okMAP.stream().mapToDouble(i -> i).toArray();
		//double[] dblArray = okMAP.stream().mapToDouble(Double::doubleValue).toArray();
		double[] dblArray = new double[okMAP.size()];
		for (int i = 0; i < dblArray.length; i++) {
			dblArray[i] = okMAP.get(i);                // java 1.5+ style (outboxing)
		}
		double[] dblArray1 = tfidfMAP.stream().mapToDouble(i -> i).toArray();
		double MAPPval = ttest.pairedTTest(dblArray, dblArray1);
		double MAPWilcoxon = wilcoxon.wilcoxonSignedRankTest(dblArray, dblArray1, false);
//		for (int i = 0; i < okMAP.size(); i++) {
//			System.out.println("okMAP: " + dblArray[i] + " tfidfMAP: " + dblArray1[i]);
//		}
		System.out.println("ttest of MAP : " + MAPPval);
		System.out.println("wilcoxon of MAP : " + MAPWilcoxon);

		// paired t test for P@k
		dblArray = okPK.stream().mapToDouble(i -> i).toArray();
		dblArray1 = tfidfPK.stream().mapToDouble(i -> i).toArray();
		double PKPval = ttest.pairedTTest(dblArray, dblArray1);
		double PKWilcoxon = wilcoxon.wilcoxonSignedRankTest(dblArray, dblArray1, false);
		System.out.println("ttest of P@k : " + PKPval);
		System.out.println("wilcoxon of P@k : " + PKWilcoxon);

		// paired t test for MRR
		dblArray = okMRR.stream().mapToDouble(i -> i).toArray();
		dblArray1 = tfidfMRR.stream().mapToDouble(i -> i).toArray();
		double MRRPval = ttest.pairedTTest(dblArray, dblArray1);
		double MRRWilcoxon = wilcoxon.wilcoxonSignedRankTest(dblArray, dblArray1, false);
		System.out.println("ttest of MRR : " + MRRPval);
		System.out.println("wilcoxon of MRR : " + MRRWilcoxon);

		// paired t test for NDCG
		dblArray = okNDCG.stream().mapToDouble(i -> i).toArray();
		dblArray1 = tfidfNDCG.stream().mapToDouble(i -> i).toArray();
		double NDCGPval = ttest.pairedTTest(dblArray, dblArray1);
		double NDCGWilcoxon = wilcoxon.wilcoxonSignedRankTest(dblArray, dblArray1, false);
		System.out.println("ttest of NDCG : " + NDCGPval);
		System.out.println("wilcoxon of NDCG : " + NDCGWilcoxon);

	}
    
	//Please implement P@K, MRR and NDCG accordingly
	public void evaluate(String method, String indexPath, String judgeFile) throws IOException {		
		_searcher = new Searcher(indexPath);		
		setSimilarity(_searcher, method);
		
		BufferedReader br = new BufferedReader(new FileReader(judgeFile));
		String line = null, judgement = null;
		int k = 10;
		double meanAvgPrec = 0.0, p_k = 0.0, mRR = 0.0, nDCG = 0.0;
		double numQueries = 0.0;
		while ((line = br.readLine()) != null) {
			judgement = br.readLine();
			
			//compute corresponding AP
			double AP = AvgPrec(line, judgement);
			meanAvgPrec += AP;
			//compute corresponding P@K
			double PK = Prec(line, judgement, k);
			p_k += PK;
//			//compute corresponding MRR
			double RR1 = RR(line, judgement);
			mRR += RR1;
//			//compute corresponding NDCG
			double NDCG1 = NDCG(line, judgement, k);
			nDCG += NDCG1;

			if (method.equals("--ok")) {
				okMAP.add(AP);
				okPK.add(PK);
				okMRR.add(RR1);
				okNDCG.add(NDCG1);
			}
			else if(method.equals("--tfidf")) {
				tfidfMAP.add(AP);
				tfidfPK.add(PK);
				tfidfMRR.add(RR1);
				tfidfNDCG.add(NDCG1);
			}
			
			++numQueries;
		}
		br.close();

		System.out.println("\nMAP: " + meanAvgPrec / numQueries);//this is the final MAP performance of your selected ranker
		System.out.println("\nP@" + k + ": " + p_k / numQueries);//this is the final P@K performance of your selected ranker
		System.out.println("\nMRR: " + mRR / numQueries);//this is the final MRR performance of your selected ranker
		System.out.println("\nNDCG: " + nDCG / numQueries); //this is the final NDCG performance of your selected ranker
		System.out.println(" ");
	}

	double AvgPrec(String query, String docString) {
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned

		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i = 1;
		double avgp = 0.0;
		double numRel = 0;
		//System.out.println("\nQuery: " + query);
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				//how to accumulate average precision (avgp) when we encounter a relevant document
				numRel ++;
				double precision = numRel / i;
				avgp += precision;
				//System.out.print("  ");
			} else {
				//how to accumulate average precision (avgp) when we encounter an irrelevant document
				//System.out.print("X ");
			}
			//System.out.println(i + ". " + rdoc.title());
			++i;
		}
		
		//compute average precision here
		//if (numRel == 0) return 0;
		avgp = avgp / relDocs.size();
		//System.out.println("Average Precision: " + avgp);
		return avgp;
	}
	
	//precision at K
	double Prec(String query, String docString, int k) {
		double p_k = 0;
		//your code for computing precision at K here
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned
		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i = 1;
		double numRel = 0;
//		System.out.println("\nQuery: " + query);
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				numRel ++;
//				System.out.print("  ");
			} else {
//				System.out.print("X ");
			}
//			System.out.println(i + ". " + rdoc.title());
			++i;

			if (i > k) break;
		}

		//compute p@k here
		if (numRel == 0) return 0;
		p_k = numRel / k;
//		System.out.println("P@"+k+": " + p_k);
		return p_k;
	}
	
	//Reciprocal Rank
	double RR(String query, String docString) {
		double rr = 0;
		//your code for computing Reciprocal Rank here
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned
		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
		int i = 1;
		double numRel = 0;
//		System.out.println("\nQuery: " + query);
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				numRel ++;
//				System.out.print("  ");
			} else {
//				System.out.print("X ");
			}
//			System.out.println(i + ". " + rdoc.title());
			if (numRel == 1) break;
			++i;
		}
		//compute reciprocal rank here
		rr = numRel / i;
//		System.out.println("rr: " + rr);

		return rr;
	}
	
	//Normalized Discounted Cumulative Gain
	double NDCG(String query, String docString, int k) {
		double ndcg = 0;
		//your code for computing Normalized Discounted Cumulative Gain here
		ArrayList<ResultDoc> results = _searcher.search(query).getDocs();
		if (results.size() == 0)
			return 0; // no result returned
		HashSet<String> relDocs = new HashSet<String>(Arrays.asList(docString.trim().split("\\s+")));
//		for (String relDoc : relDocs) {
//			System.out.println(relDoc);
//		}

		int i = 1;
		double numRel = 0;
//		System.out.println("\nQuery: " + query);
		for (ResultDoc rdoc : results) {
			if (relDocs.contains(rdoc.title())) {
				numRel ++;
//				System.out.print("  ");

				// add to ndcg now
				ndcg += 1 / (Math.log(i + 1) / Math.log(2));
			} else {
//				System.out.print("X ");
			}
//			System.out.println(i + ". " + rdoc.title());

			++i;
			if (i > k) break;
		}

		int idcgSize = Math.min(k, relDocs.size());
		double idcg = 0.0;
		for (int j = 1; j <= idcgSize; j++) {
			idcg += 1 / (Math.log(j+1)/Math.log(2));
		}

		ndcg = ndcg / idcg;
		//compute reciprocal rank here
		//System.out.println("idcg: " + idcg);
		//System.out.println("ndcg: " + ndcg);

		return ndcg;
	}
}