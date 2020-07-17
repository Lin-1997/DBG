//import java.util.*;
//import java.io.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//class DBG
//{
//	static class Read
//	{
//		String string;
//		int ref = 0;
//
//		public Read (String string)
//		{
//			this.string = string;
//		}
//	}
//
//	static class Node
//	{
//		String mer;
//		int inD = 0;
//		int outD = 0;
//
//		public Node (String mer)
//		{
//			this.mer = mer;
//		}
//	}
//
//	ConcurrentHashMap<String, Node> N = new ConcurrentHashMap<> (); //map mer to Node
//	ConcurrentHashMap<String, Read> R = new ConcurrentHashMap<> (); //map read to Read
//	ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> F = new ConcurrentHashMap<> (); //multi-map from Node's mer to Forward Nodes' mer
//	ConcurrentHashMap<String, ArrayList<String>> B = new ConcurrentHashMap<> (); //multi-map from Node's mer to Backward Nodes' mer
//	ConcurrentHashMap<String, ArrayList<String>> O = new ConcurrentHashMap<> (); //multi-map from Node's mer to Original Reads' read
//	int k, debugMode;
//
//	public DBG (int k, int debugMode)
//	{
//		this.k = k;
//		this.debugMode = debugMode;
//	}
//
//	public void createNode (String[] readList)
//	{
//		for (String read : readList)
//		{
//			if (R.containsKey (read))
//				continue;
//			debug ("---------");
//			debug ("read is" + read);
//			R.put (read, new Read (read));
//
//			for (String mer : chop (read, k)) //分成k-mer
//			{
//				String LMer = mer.substring (0, mer.length () - 1);
//				String RMer = mer.substring (1);
//				if (!N.containsKey (LMer))
//					N.put (LMer, new Node (LMer));
//				if (addRead (LMer, read)) //没有用到这个read
//				{
//					R.remove (read);
//					break;
//				}
//				if (!N.containsKey (RMer))
//					N.put (RMer, new Node (RMer));
//				addRead (RMer, read);
//				addEdge (LMer, RMer); //相互连接
//				++N.get (LMer).outD;
//				++N.get (RMer).inD;
//			}
//		}
//	}
//
//	static String[] chop (String read, int k)
//	{
//		int size = read.length () - k + 1;
//		String[] merList = new String[size];
//		for (int i = 0; i < size; ++i)
//			merList[i] = read.substring (i, i + k);
//
//		return merList;
//	}
//
//	void debug (String msg)
//	{
//		if (debugMode == 1)
//			System.out.println (msg);
//	}
//
//	boolean addRead (String mer, String read)
//	{
//		if (!O.containsKey (mer))
//		{
//			debug (mer + " add " + read + " as first read");
//			O.put (mer, new ArrayList<> (Collections.singleton (read)));
//			++R.get (read).ref;
//		}
//		else
//		{
//			ArrayList<Integer> DList = new ArrayList<> ();
//			for (int RIndex = 0; RIndex < O.get (mer).size (); ++RIndex)
//			{
//				String tempString = O.get (mer).get (RIndex);
//				if (read.length () < tempString.length ()) //当前read被已有的read包含，直接跳过整个read
//				{
//					if (tempString.contains (read))
//					{
//						debug ("skip whole " + read + " has " + tempString);
//						return true;
//					}
//				}
//				else if (read.length () == tempString.length ()) //下一个LMer与上一个RMer重复
//				{
//					if (read.equals (tempString))
//					{
//						debug (mer + " skip has same " + read);
//						return false;
//					}
//				}
//				else if (read.contains (tempString)) //当前read包含已有的read，清理掉
//					DList.add (RIndex);
//			}
//			for (int i = DList.size () - 1; i >= 0; --i) //从后往前删除
//			{
//				int DIndex = DList.get (i);
//				String DRead = O.get (mer).get (DIndex);
//				debug (mer + " del " + DRead + " use " + read);
//				cleanRead (DRead);
//				O.get (mer).remove (DIndex);
//			}
//			++R.get (read).ref;
//			O.get (mer).add (read);
//			debug (mer + " add " + read + " as read");
//		}
//		debug ("<<<<<<<");
//		for (int i = 0; i < O.get (mer).size (); ++i)
//		{
//			String tempString = O.get (mer).get (i);
//			debug (tempString);
//			debug ("" + R.get (tempString).ref);
//		}
//		debug (">>>>>>>");
//		return false;
//	}
//
//	void cleanRead (String string)
//	{
//		Read read = R.get (string);
//		--read.ref;
//		debug (read.string + " ref is " + read.ref);
//		if (read.ref == 0)
//		{
//			debug (read.string + " is useless");
//			R.remove (string);
//		}
//	}
//
//	void tryCleanRead (String string)
//	{
//		if (string.equals (""))
//			return;
//		Read read = R.get (string);
//		debug (read.string + " ref is " + read.ref);
//		if (read.ref == 0)
//		{
//			debug (read.string + " is useless");
//			R.remove (string);
//		}
//	}
//
//	void addEdge (String LMer, String RMer)
//	{
//		if (!F.containsKey (LMer))
//			F.put (LMer, new ConcurrentHashMap<> ());
//		if (F.get (LMer).containsKey (RMer))
//			F.get (LMer).replace (RMer, F.get (LMer).get (RMer) + 1);
//		else
//			F.get (LMer).put (RMer, 1);
//		if (!B.containsKey (RMer))
//			B.put (RMer, new ArrayList<> ());
//		if (!B.get (RMer).contains (LMer))
//			B.get (RMer).add (LMer);
//	}
//
//	void mergeNode (Node LNode, Node RNode)
//	{
//		//拼接LR的字符串
//		String LMer = LNode.mer, RMer = RNode.mer, newMer = LMer + RMer.substring (k - 2);
//		Node newNode = new Node (newMer); //新的Node
//		N.put (newMer, newNode);
//		newNode.inD = LNode.inD;
//		newNode.outD = RNode.outD;
//		if (B.containsKey (LMer)) //连接L的父节点
//		{
//			for (String BMer : B.get (LMer))
//			{
//				addEdge (BMer, newMer);
//				F.get (BMer).replace (newMer, F.get (BMer).get (LMer)); //复制边的权值
//				F.get (BMer).remove (LMer); //del ?-->L
//			}
//			B.remove (LMer); //del ?<--L
//		}
//		if (F.containsKey (RMer)) //连接R的子节点
//		{
//			for (String FMer : F.get (RMer).keySet ())
//			{
//				addEdge (newMer, FMer);
//				F.get (newMer).replace (FMer, F.get (RMer).get (FMer)); //复制边的权值
//				B.get (FMer).remove (RMer); //del R<--?
//			}
//			F.remove (RMer); //del R-->?
//		}
//		F.remove (LMer); //del L-->R
//		B.remove (RMer); //del L<--R
//		//更新并连上L的read
//		for (int LIndex = 0; LIndex < O.get (LMer).size (); ++LIndex)
//		{
//			String LRead = O.get (LMer).get (LIndex);
//			String newRead = ""; //到L后没有到R的都扩展到R，到L再绕圈后没有到R的也扩展
//			if (LRead.lastIndexOf (LMer) > LRead.indexOf (RMer))
//			{
//				int tempIndex = LRead.lastIndexOf (LMer);
//				newRead = LRead.substring (0, tempIndex) + newMer;
//				updateRead (LMer, newRead, LIndex);
//			}
//			addRead (newMer, O.get (LMer).get (LIndex));
//			cleanRead (LRead);
//			tryCleanRead (newRead);
//		}
//		O.remove (LMer);
//		//更新并连上R的read
//		for (int RIndex = 0; RIndex < O.get (RMer).size (); ++RIndex)
//		{
//			String RRead = O.get (RMer).get (RIndex);
//			String newRead = ""; //没有从L到R的都扩展到L
//			if (!RRead.contains (LMer) || RRead.indexOf (RMer) < RRead.indexOf (LMer))
//			{
//				int tempIndex = RRead.indexOf (RMer) + RMer.length ();
//				newRead = newMer + RRead.substring (tempIndex);
//				updateRead (RMer, newRead, RIndex);
//			}
//			addRead (newMer, O.get (RMer).get (RIndex));
//			cleanRead (RRead);
//			tryCleanRead (newRead);
//		}
//		O.remove (RMer);
//		N.remove (LMer);
//		N.remove (RMer);
//	}
//
//	void updateRead (String mer, String newRead, int index)
//	{
//		debug (O.get (mer).get (index) + " update to");
//		if (!R.containsKey (newRead))
//			R.put (newRead, new Read (newRead));
//		O.get (mer).set (index, newRead);
//		debug (O.get (mer).get (index));
//	}
//}
//
//public class Main
//{
//	public static void debug (String msg)
//	{
//		if (debugMode == 1)
//			System.out.println (msg);
//	}
//
//	public static void print (String msg)
//	{
//		System.out.println (msg);
//	}
//
//	public static String genString (int size)
//	{
//		String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//		Random random = new Random ();
//		random.setSeed (System.currentTimeMillis ());
//		StringBuilder builder = new StringBuilder ();
//		for (int i = 0; i < size; ++i)
//			builder.append (s.charAt (random.nextInt (26)));
//		return builder.toString ();
//	}
//
//	public static int debugMode = 0;
//
//	public static void main (String[] args)
//	{// TODO size
//		long startTime = System.currentTimeMillis ();
//		int Size = (int) Math.pow (10, 4),
//				Times = 6 * (int) Math.pow (10, 5),
//				BatchSize = (int) Math.pow (10, 5),
//				LengthDown = 60,
//				LengthUp = 100,
//				K = 31;
////		int Size = 15,
////				Times = 50,
////				BatchSize = 25,
////				LengthDown = 7,
////				LengthUp = 9,
////				K = 5;
//
//		DBG graph = new DBG (K, debugMode);
//
//		String origin = genString (Size);
////		String origin = "ABCDEFGHIJKLMNO";
////		String origin = "ABCDBCEF";
//		Random random = new Random ();
//		random.setSeed (startTime);
//		int BatchCount = Times / BatchSize;
//		for (int i = 0; i < BatchCount; ++i)
//		{
//			print ("in batch " + (i + 1) + " of " + BatchCount);
//			ArrayList<String> readList = new ArrayList<> ();
//			for (int j = 0; j < BatchSize; ++j)
//			{
//				int startIndex = random.nextInt (Size - LengthDown + 1);
//				int endIndex = Math.min (Size, LengthDown + random.nextInt
//						(LengthUp - LengthDown + 1) + startIndex);
//				readList.add (origin.substring (startIndex, endIndex));
//			}
//			String[] reads = new String[readList.size ()];
//			readList.toArray (reads);
//			graph.createNode (reads);
//			readList.clear ();
//			System.gc ();
//		}
//
//		print ("|forward| = " + graph.F.size ());
//		print ("|nodes| = " + graph.N.size ());
//		print ("|reads| = " + graph.R.size ());
//		long midTime = System.currentTimeMillis ();
//		print ("createNode cost is " + (double) (midTime - startTime) / 1000);
//
//		int Round = 0;
//		boolean flag = true;
//		ArrayList<String> merged = new ArrayList<> ();
//		while (flag)
//		{
//			flag = false;
//			print ("in round " + ++Round);
//			for (String LMer : graph.F.keySet ())
//			{
//				if (merged.contains (LMer))
//				{
//					debug (LMer + " is merged");
//					continue;
//				}
//				DBG.Node LNode = graph.N.get (LMer);
//				debug ("-----------------");
//				debug (LNode.inD + " inD");
//				debug (LNode.outD + " outD");
//				debug (LMer);
//				for (String RMer : graph.F.get (LMer).keySet ())
//				{
//					debug ("--" + graph.F.get (LMer).get (RMer) + "-->");
//					debug (RMer.substring (0, graph.k - 2) + " ++ " +
//							RMer.substring (graph.k - 2));
//				}
//				if (graph.F.get (LMer).size () == 1) //L只有一个出口
//					for (String RMer : graph.F.get (LMer).keySet ())
//					{
//						DBG.Node RNode = graph.N.get (RMer);
//						//L出度=R入度，R只有一个入口，非环
//						if (LNode.outD == RNode.inD && !LMer.equals (RMer))
//						{
//							debug ("merge_node=");
//							debug (LMer + RMer.substring (graph.k - 2));
//							graph.mergeNode (LNode, RNode); //合并，会改变graph.F
//							flag = true;
//							merged.add (RMer);
//						}
//					}
//			}
//			System.gc ();
//			print ("|forward| = " + graph.F.size ());
//			print ("|nodes| = " + graph.N.size ());
//			print ("|reads| = " + graph.R.size ());
//			long roundTime = System.currentTimeMillis ();
//			print ("round cost is " + (double) (roundTime - midTime) / 1000);
//			midTime = roundTime;
//		}
//		merged.clear ();
//		System.gc ();
//
//		try
//		{
//			BufferedWriter writer = new BufferedWriter (new FileWriter ("log.txt"));
//			print ("-------node-------");
//			for (String mer : graph.N.keySet ())
//			{
//				print ("--------------");
//				print ("is equal " + mer.equals (origin));
//				writer.write ("is equal " + mer.equals (origin));
//				writer.newLine ();
//				debug (graph.N.get (mer).inD + " inD");
//				debug (graph.N.get (mer).outD + " outD");
//				debug (mer);
//				debug ("-------forward-------");
//				if (graph.F.containsKey (mer))
//					for (String FMer : graph.F.get (mer).keySet ())
//					{
//						debug ("--" + graph.F.get (mer).get (FMer) + "-->");
//						debug (FMer);
//					}
//				debug ("-------backward-------");
//				if (graph.B.containsKey (mer))
//					for (String BMer : graph.B.get (mer))
//						debug (BMer);
//				debug ("-------read-------");
//				for (String read : graph.O.get (mer))
//					debug (read);
//			}
//			debug ("-------all read-------");
//			for (DBG.Read read : graph.R.values ())
//			{
//				debug ("--------------");
//				debug (read.string);
//				debug ("" + read.ref);
//			}
//			long endTime = System.currentTimeMillis ();
//			print ("end is " + endTime);
//			print ("cost is " + (double) (endTime - startTime) / 1000);
//			writer.write ("" + (double) (endTime - startTime) / 1000);
//			writer.newLine ();
//			writer.close ();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace ();
//		}
//	}
//}

import java.util.*;
import java.io.*;
import java.util.concurrent.*;

class DBG
{
	static class Read
	{
		String string;
		int ref = 0;
		int locked = 0;

		public Read (String string)
		{
			this.string = string;
		}
	}

	static class Node
	{
		String mer;
		int inD = 0;
		int outD = 0;
		boolean locked = false;

		public Node (String mer)
		{
			this.mer = mer;
		}
	}

	static String[] chop (String read, int k)
	{
		int size = read.length () - k + 1;
		String[] merList = new String[size];
		for (int i = 0; i < size; ++i)
			merList[i] = read.substring (i, i + k);
		return merList;
	}

	void debug (String msg)
	{
		if (debugMode == 1)
			System.out.println (Thread.currentThread ().getName () + "--->" + msg);
	}

	final ConcurrentHashMap<String, Node> N = new ConcurrentHashMap<> (); //map mer to Node
	final ConcurrentHashMap<String, Read> R = new ConcurrentHashMap<> (); //map read to Read
	final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> F = new ConcurrentHashMap<> (); //multi-map from Node's mer to Forward Nodes' mer
	final ConcurrentHashMap<String, ArrayList<String>> B = new ConcurrentHashMap<> (); //multi-map from Node's mer to Backward Nodes' mer
	final ConcurrentHashMap<String, ArrayList<String>> O = new ConcurrentHashMap<> (); //multi-map from Node's mer to Original Reads' read
	final int k, debugMode;

	public DBG (int k, int debugMode)
	{
		this.k = k;
		this.debugMode = debugMode;
	}

	void createNode (String[] readList)
	{
		for (String read : readList)
		{
			if (R.putIfAbsent (read, new Read (read)) != null)
				continue;
			//对相同的read只有一个线程能下来
			debug ("---------");
			debug ("read is " + read);
			for (String mer : chop (read, k)) //分成k-mer
			{
				String LMer = mer.substring (0, mer.length () - 1);
				String RMer = mer.substring (1);
				N.putIfAbsent (LMer, new Node (LMer));
				if (addRead (LMer, read, false)) //没有用到这个read
				{
					R.remove (read);
					break;
				}
				N.putIfAbsent (RMer, new Node (RMer));
				addRead (RMer, read, false);
				addEdge (LMer, RMer); //相互连接
				synchronized (N.get (LMer))
				{
					++N.get (LMer).outD;
				}
				synchronized (N.get (RMer))
				{
					++N.get (RMer).inD;
				}
			}
		}
	}

	boolean addRead (String mer, String read, boolean atMerge)
	{
		if (O.putIfAbsent (mer, new ArrayList<> (Collections.singleton (read))) == null)
		{
			debug (mer + " add " + read + " as first read");
			// 如果被删了，只要不++ref就行了，read肯定会被其他线程替换的
			try
			{
				synchronized (R.get (read))
				{
					++R.get (read).ref;
					if (atMerge)
						++R.get (read).locked;
					debug (read + " ref is " + R.get (read).ref + ", locked is " + R.get (read).locked);
				}
			}
			catch (Exception e)
			{
				debug (" skip whole " + read + " deleted in other Thread");
				return true;
			}
		}
		else
		{
			ArrayList<Integer> DList = new ArrayList<> ();
			synchronized (O.get (mer))
			{
				for (int RIndex = 0; RIndex < O.get (mer).size (); ++RIndex)
				{
					String tempString = O.get (mer).get (RIndex);
					if (read.length () < tempString.length ()) //当前read被已有的read包含，直接跳过整个read
					{
						if (tempString.contains (read))
						{
							debug ("skip whole " + read + " has " + tempString);
							return true;
						}
					}
					else if (read.length () == tempString.length ()) //下一个LMer与上一个RMer重复
					{
						if (read.equals (tempString))
						{
							debug (mer + " skip has same " + read);
							return false;
						}
					}
					else if (read.contains (tempString)) //当前read包含已有的read，清理掉
						DList.add (RIndex);
				}
				for (int i = DList.size () - 1; i >= 0; --i) //从后往前删除
				{
					int DIndex = DList.get (i);
					String DRead = O.get (mer).get (DIndex);
					debug (mer + " del " + DRead + " use " + read);
					cleanRead (DRead, atMerge);
					O.get (mer).remove (DIndex);
				}
				O.get (mer).add (read);
			} //synchronized (O.get (mer))
			try
			{
				synchronized (R.get (read))
				{
					++R.get (read).ref;
					if (atMerge)
						++R.get (read).locked;
					debug (read + " ref is " + R.get (read).ref + ", locked is " + R.get (read).locked);
				}
			}
			catch (Exception e)
			{
				debug (" skip whole " + read + " deleted in other Thread");
				return true;
			}
			debug (mer + " add " + read + " as read");
		}
//		debug ("<<<<<<<");
//		for (int i = 0; i < O.get (mer).size (); ++i)
//		{
//			String tempString = O.get (mer).get (i);
//			debug (tempString);
//			debug ("ref is " + R.get (tempString).ref);
//			debug ("locked is " + R.get (tempString).locked);
//		}
//		debug (">>>>>>>");
		return false;
	}

	void cleanRead (String string, boolean atMerge)
	{
		try
		{
			synchronized (R.get (string))
			{
				Read read = R.get (string);
				--read.ref;
				if (atMerge)
					--read.locked;
				debug (string + " ref is " + read.ref + ", locked is " + read.locked);
				if (read.ref == 0)
					R.remove (string);
			}
		}
		catch (Exception e)
		{
			debug ("read " + string + " deleted in other Thread");
		}
	}

	void tryCleanRead (String string)
	{
		if (string.equals (""))
			return;
		try
		{
			synchronized (R.get (string))
			{
				Read read = R.get (string);
				debug (string + " ref is " + read.ref + ", locked is " + read.locked);
				if (read.ref == 0)
					R.remove (string);
			}
		}
		catch (Exception e)
		{
			debug ("read " + string + " deleted in other Thread");
		}
	}

	void addEdge (String LMer, String RMer)
	{
		F.putIfAbsent (LMer, new ConcurrentHashMap<> ());
		if (F.get (LMer).putIfAbsent (RMer, 1) != null)
			synchronized (F.get (LMer))
			{
				F.get (LMer).replace (RMer, F.get (LMer).get (RMer) + 1);
			}
		B.putIfAbsent (RMer, new ArrayList<> ());
		synchronized (B.get (RMer))
		{
			if (!B.get (RMer).contains (LMer))
				B.get (RMer).add (LMer);
		}
	}

	void updateLEdge (String BMer, String newMer, String LMer)
	{
		synchronized (F.get (BMer))
		{
			F.get (BMer).put (newMer, F.get (BMer).get (LMer));
			F.get (BMer).remove (LMer);
		}
		B.put (newMer, new ArrayList<> (Collections.singleton (BMer)));
	}

	void updateREdge (String newMer, String FMer, String RMer)
	{
		F.put (newMer, new ConcurrentHashMap<> (Collections.singletonMap
				(FMer, F.get (RMer).get (FMer))));
		synchronized (B.get (FMer))
		{
			B.get (FMer).add (newMer);
			B.get (FMer).remove (RMer);
		}
	}


//	void mergeNode (Node LNode, Node RNode)
//	{
//		//拼接LR的字符串
//		String LMer = LNode.mer, RMer = RNode.mer, newMer = LMer + RMer.substring (k - 2);
//		Node newNode = new Node (newMer); //新的Node
//		N.put (newMer, newNode);
//		newNode.inD = LNode.inD;
//		newNode.outD = RNode.outD;
//		if (B.containsKey (LMer)) //连接L的父节点
//		{
//			for (String BMer : B.get (LMer))
//			{
//				addEdge (BMer, newMer);
//				F.get (BMer).replace (newMer, F.get (BMer).get (LMer)); //复制边的权值
//				F.get (BMer).remove (LMer); //del ?-->L
//			}
//			B.remove (LMer); //del ?<--L
//		}
//		if (F.containsKey (RMer)) //连接R的子节点
//		{
//			for (String FMer : F.get (RMer).keySet ())
//			{
//				addEdge (newMer, FMer);
//				F.get (newMer).replace (FMer, F.get (RMer).get (FMer)); //复制边的权值
//				B.get (FMer).remove (RMer); //del R<--?
//			}
//			F.remove (RMer); //del R-->?
//		}
//		F.remove (LMer); //del L-->R
//		B.remove (RMer); //del L<--R
//		//更新并连上L的read
//		for (int LIndex = 0; LIndex < O.get (LMer).size (); ++LIndex)
//		{
//			String LRead = O.get (LMer).get (LIndex);
//			String newRead = ""; //到L后没有到R的都扩展到R，到L再绕圈后没有到R的也扩展
//			if (LRead.lastIndexOf (LMer) > LRead.indexOf (RMer))
//			{
//				int tempIndex = LRead.lastIndexOf (LMer);
//				newRead = LRead.substring (0, tempIndex) + newMer;
//				updateRead (LMer, newRead, LIndex);
//			}
//			addRead (newMer, O.get (LMer).get (LIndex));
//			cleanRead (LRead);
//			tryCleanRead (newRead);
//		}
//		O.remove (LMer);
//		//更新并连上R的read
//		for (int RIndex = 0; RIndex < O.get (RMer).size (); ++RIndex)
//		{
//			String RRead = O.get (RMer).get (RIndex);
//			String newRead = ""; //没有从L到R的都扩展到L
//			if (!RRead.contains (LMer) || RRead.indexOf (RMer) < RRead.indexOf (LMer))
//			{
//				int tempIndex = RRead.indexOf (RMer) + RMer.length ();
//				newRead = newMer + RRead.substring (tempIndex);
//				updateRead (RMer, newRead, RIndex);
//			}
//			addRead (newMer, O.get (RMer).get (RIndex));
//			cleanRead (RRead);
//			tryCleanRead (newRead);
//		}
//		O.remove (RMer);
//		N.remove (LMer);
//		N.remove (RMer);
//	}
//
//	void updateRead (String mer, String newRead, int index)
//	{
//		debug (O.get (mer).get (index) + " update to");
//		if (!R.containsKey (newRead))
//			R.put (newRead, new Read (newRead));
//		O.get (mer).set (index, newRead);
//		debug (O.get (mer).get (index));
//	}

	boolean mergeNode (Node LNode, Node RNode)
	{
		synchronized (R)
		{
			for (String read : O.get (LNode.mer))
				if (R.get (read).locked > 0)
				{
					debug (read + " is locked");
					return false;
				}
			for (String read : O.get (RNode.mer))
				if (R.get (read).locked > 0)
				{
					debug (read + " is locked");
					return false;
				}
			for (String read : O.get (LNode.mer))
			{
				debug ("lock " + read);
				++R.get (read).locked;
			}
			for (String read : O.get (RNode.mer))
			{
				debug ("lock " + read);
				++R.get (read).locked;
			}
		}

		//拼接LR的字符串
		String LMer = LNode.mer, RMer = RNode.mer, newMer = LMer + RMer.substring (k - 2);
		Node newNode = new Node (newMer); //新的Node
		N.put (newMer, newNode);
		newNode.inD = LNode.inD;
		newNode.outD = RNode.outD;
		if (B.containsKey (LMer)) //连接L的父节点
		{
			for (String BMer : B.get (LMer))
				updateLEdge (BMer, newMer, LMer); //?<-->new, del ?-->L
			B.remove (LMer); //del ?<--L
		}
		if (F.containsKey (RMer)) //连接R的子节点
		{
			for (String FMer : F.get (RMer).keySet ())
				updateREdge (newMer, FMer, RMer); //new<-->?, del R-->?
			F.remove (RMer); //del R-->?
		}
		F.remove (LMer); //del L-->R
		B.remove (RMer); //del L<--R
		//更新并连上L的read
		for (int LIndex = 0; LIndex < O.get (LMer).size (); ++LIndex)
		{
			String LRead = O.get (LMer).get (LIndex);
			//到L后没有到R的都扩展到R，到L再绕圈后没有到R的也扩展
			if (LRead.lastIndexOf (LMer) > LRead.indexOf (RMer))
			{
				String newRead = LRead.substring
						(0, LRead.lastIndexOf (LMer)) + newMer;
				R.putIfAbsent (newRead, new Read (newRead));
				addRead (newMer, newRead, true);
				tryCleanRead (newRead);
			}
			else
				addRead (newMer, LRead, true);
			cleanRead (LRead, true);
		}
		O.remove (LMer);
		//更新并连上R的read
		for (int RIndex = 0; RIndex < O.get (RMer).size (); ++RIndex)
		{
			String RRead = O.get (RMer).get (RIndex);
			//没有从L到R的都扩展到L
			if (!RRead.contains (LMer) || RRead.indexOf (RMer) < RRead.indexOf (LMer))
			{
				String newRead = newMer + RRead.substring
						(RRead.indexOf (RMer) + RMer.length ());
				R.putIfAbsent (newRead, new Read (newRead));
				addRead (newMer, newRead, true);
				tryCleanRead (newRead);
			}
			else
				addRead (newMer, RRead, true);
			cleanRead (RRead, true);
		}
		O.remove (RMer);
		N.remove (LMer);
		N.remove (RMer);

		synchronized (R)
		{
			for (String read : O.get (newMer))
			{
				debug ("unlock " + read);
				--R.get (read).locked;
			}
		}
		return true;
	}
}

public class Main
{
	public static void debug (String msg)
	{
		if (debugMode == 1)
			System.out.println (Thread.currentThread ().getName () + "--->" + msg);
	}

	public static void print (String msg)
	{
		System.out.println (Thread.currentThread ().getName () + "--->" + msg);
	}

	public static String genString (int size)
	{
		String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random random = new Random ();
		random.setSeed (System.currentTimeMillis ());
		StringBuilder builder = new StringBuilder ();
		for (int i = 0; i < size; ++i)
			builder.append (s.charAt (random.nextInt (26)));
		return builder.toString ();
	}

	public static int debugMode = 0;

	public static void main (String[] args)
	{// TODO size
		long startTime = System.currentTimeMillis ();
//		int Size = (int) Math.pow (10, 4),
//				Times = 6 * (int) Math.pow (10, 5),
//				BatchSize = (int) Math.pow (10, 5),
//				LengthDown = 60,
//				LengthUp = 100,
//				K = 31;
		int Size = 15,
				Times = 30,
				BatchSize = 5,
				LengthDown = 7,
				LengthUp = 15,
				K = 5;
//		String origin = genString (Size);
		String origin = "ABCDEFGHIJKLMNO";
//		String origin = "ABCDBCEF";
		Random random = new Random ();
		random.setSeed (startTime);
		int BatchCount = Times / BatchSize;

		DBG graph = new DBG (K, debugMode);

		int threadCount = Math.min (Runtime.getRuntime ().availableProcessors () - 1, BatchCount);
		ExecutorService executor = Executors.newFixedThreadPool (threadCount);
		CountDownLatch count = new CountDownLatch (BatchCount);
		for (int i = 0; i < BatchCount; ++i)
		{
			int finalI = i;
			executor.submit (() ->
			{
				print ("in batch " + (finalI + 1) + " of " + BatchCount);
				ArrayList<String> readList = new ArrayList<> ();
				for (int j = 0; j < BatchSize; ++j)
				{
					int startIndex = random.nextInt (Size - LengthDown + 1);
					int endIndex = Math.min (Size, LengthDown + random.nextInt
							(LengthUp - LengthDown + 1) + startIndex);
					readList.add (origin.substring (startIndex, endIndex));
				}
				String[] reads = new String[readList.size ()];
				readList.toArray (reads);
				graph.createNode (reads);
				readList.clear ();
				System.gc ();
				count.countDown ();
			});
		}

		try
		{
			count.await ();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace ();
		}

		print ("|forward| = " + graph.F.size ());
		print ("|nodes| = " + graph.N.size ());
		print ("|reads| = " + graph.R.size ());
		long midTime = System.currentTimeMillis ();
		print ("createNode cost is " + (double) (midTime - startTime) / 1000);

		int Round = 1;
		boolean flag = true;
		while (flag)
		{
			flag = false;
			CountDownLatch tempCount = new CountDownLatch (graph.F.size ());
			print ("in round " + Round++);
			for (String LMer : new ArrayList<> (graph.F.keySet ()))
			{
				DBG.Node LNode = graph.N.get (LMer);
				if (LNode == null || LNode.locked)
				{
					debug (LMer + " is merged or locked");
					tempCount.countDown ();
					debug ("!LNode count " + tempCount.getCount ());
					continue;
				}
				if (graph.F.get (LMer).size () != 1)
					tempCount.countDown ();
				else //L只有一个出口
					for (String RMer : graph.F.get (LMer).keySet ())
						if (graph.B.get (RMer).size () != 1 || LMer.equals (RMer))
							tempCount.countDown ();
						else //R只有一个入口，非环
						{
							DBG.Node RNode = graph.N.get (RMer);
							if (RNode == null || RNode.locked)
								tempCount.countDown ();
							else
							{
								LNode.locked = RNode.locked = true;
								executor.submit (() ->
								{
									debug ("merge " + LMer + " + " + RMer + " = " + LMer + RMer.substring (graph.k - 2));
									if (!graph.mergeNode (LNode, RNode))
										LNode.locked = RNode.locked = false;
									tempCount.countDown ();
								});
							}
							flag = true;
						}
			}

			if (flag)
				try
				{
					tempCount.await ();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace ();
				}
			System.gc ();
			print ("|forward| = " + graph.F.size ());
			print ("|nodes| = " + graph.N.size ());
			print ("|reads| = " + graph.R.size ());
			long roundTime = System.currentTimeMillis ();
			print ("round cost is " + (double) (roundTime - midTime) / 1000);
			midTime = roundTime;
		}
		executor.shutdown ();
		System.gc ();

		//TODO 处理环
		try
		{
			BufferedWriter writer = new BufferedWriter (new FileWriter ("log.txt"));
			print ("-------node-------");
			for (String mer : graph.N.keySet ())
			{
				print ("--------------");
				print ("is equal " + mer.equals (origin));
				writer.write ("is equal " + mer.equals (origin));
				writer.newLine ();
				debug (graph.N.get (mer).inD + " inD");
				debug (graph.N.get (mer).outD + " outD");
				debug (mer);
				debug ("-------forward-------");
				if (graph.F.containsKey (mer))
					for (String FMer : graph.F.get (mer).keySet ())
					{
						debug ("--" + graph.F.get (mer).get (FMer) + "-->");
						debug (FMer);
					}
				debug ("-------backward-------");
				if (graph.B.containsKey (mer))
					for (String BMer : graph.B.get (mer))
						debug (BMer);
				debug ("-------read-------");
				for (String read : graph.O.get (mer))
					debug (read);
			}
			debug ("-------all read-------");
			for (DBG.Read read : graph.R.values ())
			{
				debug ("--------------");
				debug (read.string);
				debug ("" + read.ref);
			}
			long endTime = System.currentTimeMillis ();
			print ("end is " + endTime);
			print ("cost is " + (double) (endTime - startTime) / 1000);
			writer.write ("" + (double) (endTime - startTime) / 1000);
			writer.newLine ();
			writer.close ();
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
	}
}
