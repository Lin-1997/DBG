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

		public Node (String mer, boolean locked)
		{
			this.mer = mer;
			this.locked = locked;
		}
	}

	final ConcurrentHashMap<String, Node> N = new ConcurrentHashMap<> (); //map mer to Node
	final ConcurrentHashMap<String, Read> R = new ConcurrentHashMap<> (); //map read to Read
	final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> F = new ConcurrentHashMap<> (); //multi-map from Node's mer to Forward Nodes' mer
	final ConcurrentHashMap<String, ArrayList<String>> B = new ConcurrentHashMap<> (); //multi-map from Node's mer to Backward Nodes' mer
	final ConcurrentHashMap<String, ArrayList<String>> O = new ConcurrentHashMap<> (); //multi-map from Node's mer to Original Reads' read
	int k;

	public DBG (int k)
	{
		this.k = k;
	}

	void createNode (String[] readList)
	{
		for (String read : readList)
		{
			String mer, LMer, RMer;
			for (int i = 0; i < read.length () - k + 1; i++)
			{
				mer = read.substring (i, i + k);
				LMer = mer.substring (0, mer.length () - 1);
				RMer = mer.substring (1);
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
		if (O.putIfAbsent (mer, new ArrayList<> (Collections.singleton (read))) != null)
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
							return true;
					}
					else if (read.length () == tempString.length ()) //下一个LMer与上一个RMer重复
					{
						if (read.equals (tempString))
							return false;
					}
					else if (read.contains (tempString)) //当前read包含已有的read，清理掉
						DList.add (RIndex);
				}
				for (int i = DList.size () - 1; i >= 0; --i) //从后往前删除
				{
					int DIndex = DList.get (i);
					String DRead = O.get (mer).get (DIndex);
					cleanRead (DRead, atMerge);
					O.get (mer).remove (DIndex);
				}
				O.get (mer).add (read);
			} //synchronized (O.get (mer))
		}
		try
		{
			synchronized (R.get (read))
			{
				++R.get (read).ref;
				if (atMerge)
					++R.get (read).locked;
			}
		}
		catch (Exception e)
		{
			return true;
		}
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
				if (read.ref == 0)
					R.remove (string);
			}
		}
		catch (Exception ignored)
		{
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
				if (read.ref == 0)
					R.remove (string);
			}
		}
		catch (Exception ignored)
		{
		}
	}

	void addEdge (String LMer, String RMer)
	{
		synchronized (F)
		{
			F.putIfAbsent (LMer, new ConcurrentHashMap<> ());
			if (F.get (LMer).putIfAbsent (RMer, 1) != null)
				F.get (LMer).replace (RMer, F.get (LMer).get (RMer) + 1);
		}
		synchronized (B)
		{
			B.putIfAbsent (RMer, new ArrayList<> ());
			if (!B.get (RMer).contains (LMer))
				B.get (RMer).add (LMer);
		}
	}

	void updateLEdge (String BMer, String newMer, String LMer)
	{
		F.get (BMer).put (newMer, F.get (BMer).get (LMer));
		F.get (BMer).remove (LMer);
		B.putIfAbsent (newMer, new ArrayList<> ());
		if (!B.get (newMer).contains (BMer))
			B.get (newMer).add (BMer);
	}

	void updateREdge (String newMer, String FMer, String RMer)
	{
		F.putIfAbsent (newMer, new ConcurrentHashMap<> ());
		F.get (newMer).put (FMer, F.get (RMer).get (FMer));
		if (!B.get (FMer).contains (newMer))
			B.get (FMer).add (newMer);
		B.get (FMer).remove (RMer);
	}

	void mergeNode (Node LNode, Node RNode)
	{
		//拼接LR的字符串
		String LMer = LNode.mer, RMer = RNode.mer;
		String newMer = LMer + RMer.substring (k - 2);
		Node newNode = new Node (newMer, true); //新的Node
		newNode.inD = LNode.inD;
		newNode.outD = RNode.outD;
		N.put (newMer, newNode);
		synchronized (F)
		{
			synchronized (B)
			{
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
			}
		}
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
				--R.get (read).locked;
		}
	}
}

public class Main
{
	static void print (String msg)
	{
		System.out.println (msg);
	}

	static void DFS (DBG graph, String in, ArrayList<String> list)
	{
		if (graph.F.get (in) != null)
			for (String out : graph.F.get (in).keySet ())
			{
				graph.B.get (out).remove (in);
				graph.F.get (in).remove (out);
				DFS (graph, out, list);
			}
		list.add (in);
	}

	public static void main (String[] args)
	{
		long startTime = System.currentTimeMillis ();
		int BatchSize = (int) Math.pow (10, 5), K = 31;
		DBG graph = new DBG (K);

		int threadCount = Runtime.getRuntime ().availableProcessors ();
		ArrayList<String> origin = new ArrayList<> ();
		ArrayList<Future<Integer>> futures = new ArrayList<> ();
		ExecutorService executor = Executors.newFixedThreadPool (threadCount);
		try
		{
			BufferedReader reader = new BufferedReader (new FileReader ("sequence.txt"));
			String read;
			int batch = 1;
			while ((read = reader.readLine ()) != null)
			{
				if (graph.R.putIfAbsent (read, new DBG.Read (read)) != null)
					continue;
				origin.add (read);
				if (origin.size () == BatchSize)
				{
					int finalBatch = batch++;
					String[] reads = new String[origin.size ()];
					origin.toArray (reads);
					origin.clear ();
					if (finalBatch % (10 * threadCount + 1) == 0)
						for (int i = futures.size () - 1; i >= 0; --i)
						{
							futures.get (i).get ();
							futures.remove (i);
						}
					print ("submit batch " + finalBatch);
					futures.add (executor.submit (() ->
					{
						graph.createNode (reads);
						print ("finish batch " + finalBatch);
						return 1;
					}));
				}
			}
			reader.close ();
			if (origin.size () != 0)
			{
				int finalBatch = batch;
				String[] reads = new String[origin.size ()];
				origin.toArray (reads);
				origin.clear ();
				print ("submit batch " + finalBatch);
				futures.add (executor.submit (() ->
				{
					graph.createNode (reads);
					print ("finish batch " + finalBatch);
					return 1;
				}));
			}
			for (int i = futures.size () - 1; i >= 0; --i)
			{
				futures.get (i).get ();
				futures.remove (i);
			}
		}
		catch (IOException | ExecutionException | InterruptedException e)
		{
			e.printStackTrace ();
		}
		print ("|forward| = " + graph.F.size ());
		print ("|nodes| = " + graph.N.size ());
		print ("|reads| = " + graph.R.size ());

		int round, buildTimes = 0;
		boolean flag, start = true;
		while (graph.N.size () != 1 && buildTimes < 10)
		{
			++buildTimes;
			//尽人事处理一下环
			if (!start)
			{
				graph.F.clear ();
				graph.B.clear ();
				graph.O.clear ();
				int minMerSize = (int) Math.pow (10, 10), minReadSize = minMerSize;
				String[] allMer = graph.N.keySet ().toArray (new String[0]);
				graph.N.clear ();
				for (String mer : allMer)
					if (mer.length () < minMerSize)
						minMerSize = mer.length ();
				String[] allRead = graph.R.keySet ().toArray (new String[0]);
				graph.R.clear ();
				for (String read : allRead)
				{
					graph.R.putIfAbsent (read, new DBG.Read (read));
					if (read.length () < minReadSize)
						minReadSize = read.length ();
				}
				if (minMerSize > minReadSize - 3)
					break;
				int newK = Math.min (minMerSize * 3 / 2, minReadSize - 3);
				if (newK % 2 == 0)
					++newK;
				if (newK <= graph.k)
					break;
				graph.k = newK;
				print ("update k to " + graph.k);
				graph.createNode (allRead);
			}

			flag = true;
			round = 0;
			while (flag)
			{
				++round;
				if (start)
				{
					print ("in build " + buildTimes);
					print ("in round " + round);
					print ("|forward| = " + graph.F.size ());
					print ("|nodes| = " + graph.N.size ());
					print ("|reads| = " + graph.R.size ());
				}
				else if (round % 50 == 0)
				{
					print ("in build " + buildTimes);
					print ("in round " + round);
					print ("|forward| = " + graph.F.size ());
					print ("|nodes| = " + graph.N.size ());
					print ("|reads| = " + graph.R.size ());
				}

				flag = false;
				for (String LMer : graph.F.keySet ())
				{
					DBG.Node LNode = graph.N.get (LMer);
					if (LNode == null || LNode.locked)
						continue;
					synchronized (graph.R)
					{
						boolean skipMer = false;
						for (String read : graph.O.get (LMer))
							if (graph.R.get (read).locked > 0)
							{
								skipMer = true;
								flag = true;
								break;
							}
						if (skipMer)
							continue;
					}

					if (graph.F.get (LMer).size () == 1) //L只有一个出口
						for (String RMer : graph.F.get (LMer).keySet ())
						{
							DBG.Node RNode = graph.N.get (RMer);
							if (RNode != null && !LMer.equals (RMer) && graph.B.get (RMer) != null
									&& graph.B.get (RMer).size () == 1) //R只有一个入口，非环
							{
								flag = true;
								if (RNode.locked)
									continue;
								synchronized (graph.R)
								{
									boolean skipMer = false;
									for (String read : graph.O.get (RMer))
										if (graph.R.get (read).locked > 0)
										{
											skipMer = true;
											break;
										}
									if (skipMer)
										continue;

									for (String read : graph.O.get (LMer))
										++graph.R.get (read).locked;
									for (String read : graph.O.get (RMer))
										++graph.R.get (read).locked;
								}
								LNode.locked = RNode.locked = true;
								futures.add (executor.submit (() ->
								{
									String newMer = LMer + RMer.substring (graph.k - 2);
									graph.mergeNode (LNode, RNode);
									graph.N.get (newMer).locked = false;
									return 1;
								}));
							}
						}
				}
				try
				{
					for (int i = futures.size () - 1; i > 0; --i)
					{
						futures.get (i).get ();
						futures.remove (i);
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					e.printStackTrace ();
				}
			}
			start = false;
		}
		executor.shutdown ();

		print ("|forward| = " + graph.F.size ());
		print ("|nodes| = " + graph.N.size ());
		print ("|reads| = " + graph.R.size ());

		//找个入口
		String startMer = "";
		for (DBG.Node node : graph.N.values ())
			if (node.inD < node.outD)
			{
				startMer = node.mer;
				break;
			}
		//死循环，随便找个
		if (startMer.equals (""))
			startMer = graph.N.keys ().nextElement ();
		//走一遍DFS
		ArrayList<String> finalMerList = new ArrayList<> ();
		DFS (graph, startMer, finalMerList);

		//倒序拼接
		int j = finalMerList.size ();
		StringBuilder builderMer = new StringBuilder ();
		builderMer.append (finalMerList.get (j - 1));
		for (int i = 1; i < j; ++i)
			builderMer.append (finalMerList.get (j - 1 - i).substring (graph.k - 2));

		String finalMer = builderMer.toString ();
		try
		{
			BufferedWriter writer = new BufferedWriter (new FileWriter ("result.txt"));
			writer.write (finalMer);
			writer.close ();
			print ("write in result.txt");
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
		long endTime = System.currentTimeMillis ();
		print ("cost " + (endTime - startTime) / 1000 + "s");
	}
}
