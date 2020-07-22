import java.io.*;
import java.util.Random;

public class util
{
	static void gen (int Size, int Times, int LengthUp, int LengthDown) throws IOException
	{
		String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random random = new Random ();
		random.setSeed (System.currentTimeMillis ());
		StringBuilder builder = new StringBuilder ();
		String repeat = "AAAAABBBBBCCCCCDDDDDEEEEEFFFFFGGGGGHHHHHIIIIIJJJJJKKKKK";
		for (int i = 0; i < Size; ++i)
		{
			builder.append (s.charAt (random.nextInt (26)));
			if (i % 330 == 0)
			{
				builder.append (repeat);
				i += repeat.length ();
			}
		}
		String origin = builder.toString ();
		BufferedWriter writer = new BufferedWriter (new FileWriter ("origin.txt"));
		writer.write (origin);
		writer.close ();
		writer = new BufferedWriter (new FileWriter ("sequence.txt"));
		for (int i = 0, startIndex, endIndex; i < Times; ++i)
		{
			startIndex = random.nextInt (Size - LengthDown + 1);
			endIndex = Math.min (Size, LengthDown + random.nextInt
					(LengthUp - LengthDown + 1) + startIndex);
			writer.write (origin.substring (startIndex, endIndex));
			writer.write ("\r\n");
		}
		writer.close ();
		System.out.println ("write origin.txt and sequence.txt");
	}

	static boolean verify () throws IOException
	{
		BufferedReader reader = new BufferedReader (new FileReader ("origin.txt"));
		String origin = reader.readLine ();
		reader.close ();
		reader = new BufferedReader (new FileReader ("result.txt"));
		String result = reader.readLine ();
		return origin.equals (result);
	}

	public static void main (String[] args) throws IOException
	{
		int Size = (int) Math.pow (10, 4),
				Times = 6 * (int) Math.pow (10, 5),
				LengthDown = 60,
				LengthUp = 100;

		gen (Size, Times, LengthUp, LengthDown);
//		System.out.println (verify ());
	}
}
