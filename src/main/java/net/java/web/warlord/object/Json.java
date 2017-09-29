package net.java.web.warlord.object;

/* Java */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Simple JSON parse and write.
 *
 * @author Fang Yidong fangyidong@yahoo.com.cn
 * @author anton.baukin@gmail.com.
 */
@SuppressWarnings("unchecked")
public class Json
{
	/**
	 * Reads JSON from the read that is closed
	 * in any case after the parse complete.
	 *
	 * Returns LinkedHashMap, ArrayList, or
	 * a Number, String, Boolean, null.
	 */
	public static Object s2o(String s)
	{
		if(s == null || s.isEmpty())
			return null;

		try
		{
			return s2o(new StringReader(s));
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public static Object s2o(InputStream stream)
	{
		try
		{
			return s2o(new InputStreamReader(stream, "UTF-8"));
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public static Object s2o(Reader reader)
	  throws IOException
	{
		EX.assertn(reader);

		try(Reader r = reader)
		{
			return new Parser(r).parse();
		}
	}

	public static String o2s(Object o)
	{
		try
		{
			StringWriter s = new StringWriter(2048);

			o2s(o, s);
			return s.toString();
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public static void   o2s(Object o, OutputStream stream)
	{
		try
		{
			Writer w = new OutputStreamWriter(stream, "UTF-8");

			o2s(o, w);
			w.flush(); //<-- flush the endings
		}
		catch(Throwable e)
		{
			throw EX.wrap(e);
		}
	}

	public static void   o2s(Object o, Writer w)
	  throws IOException
	{
		if(o instanceof Mapped)
			o = ((Mapped)o).map();

		if(o == null)
			w.write("null");
		else if(o instanceof String)
			escape((String)o, w);
		else if(o instanceof Boolean)
			w.write(o.toString());
		else if(o instanceof Number)
			w.write(o.toString());
		else if(o instanceof List)
		{
			w.write("[");

			int i = 0, s = ((List)o).size();
			for(Object item : (List)o)
			{
				Json.o2s(item, w);
				if(++i != s)
					w.write(",");
			}

			w.write("]");
		}
		else if(o instanceof Map)
		{
			w.write("{");

			int i = 0, s = ((Map)o).size();
			for(Map.Entry<?,?> e : ((Map<?,?>)o).entrySet())
			{
				if(!(e.getKey() instanceof String))
					throw EX.ass("JSON Map key is not a string!");

				escape((String)e.getKey(), w);
				w.write(":");
				Json.o2s(e.getValue(), w);
				if(++i != s)
					w.write(",");
			}

			w.write("}");
		}
		else
			throw EX.ass("Unsupported JSON type!");
	}

	/**
	 * Ineffective way of using {@link OU#get(Object, Object...)}
	 * with JSON-encoded object.
	 */
	public static Object get(String json, Object... path)
	{
		if(json == null || json.isEmpty())
			return null;
		return OU.get(Json.s2o(json), path);
	}


	/* Write Support */

	private static void escape(String s, Writer w)
	  throws IOException
	{
		String t;
		char   c;
		int    i, len = s.length();

		w.write("\"");
		for(i = 0; i < len; i += 1)
		{
			c = s.charAt(i);
			switch (c)
			{
				case '\\':
				case '"':
					w.write('\\');
					w.write(c);
					break;
				case '/':
					w.write('\\');
					w.write(c);
					break;
				case '\b':
					w.write("\\b");
					break;
				case '\t':
					w.write("\\t");
					break;
				case '\n':
					w.write("\\n");
					break;
				case '\f':
					w.write("\\f");
					break;
				case '\r':
					w.write("\\r");
					break;
				default:
					if (c < ' ') {
						t = "000" + Integer.toHexString(c);
						w.write("\\u" + t.substring(t.length() - 4));
					} else {
						w.write(c);
					}
			}
		}
		w.write("\"");
	}


	/* JSON Simple Parser */

	private static class Parser
	{
		public static final int S_INIT = 0;
		public static final int S_IN_FINISHED_VALUE = 1;
		public static final int S_IN_OBJECT = 2;
		public static final int S_IN_ARRAY = 3;
		public static final int S_PASSED_PAIR_KEY = 4;
		public static final int S_IN_ERROR = -1;

		private Yylex lexer;
		private Yytoken token = null;
		private int status = S_INIT;

		public Parser(Reader reader)
		{
			lexer = new Yylex(reader);
		}

		public Object parse()
		  throws IOException
		{
			LinkedList statusStack = new LinkedList();
			LinkedList valueStack = new LinkedList();

			do
			{
				nextToken();
				switch(status)
				{
					case S_INIT:
						switch(token.type)
						{
							case Yytoken.TYPE_VALUE:
								status = S_IN_FINISHED_VALUE;
								statusStack.addFirst(status);
								valueStack.addFirst(token.value);
								break;
							case Yytoken.TYPE_LEFT_BRACE:
								status = S_IN_OBJECT;
								statusStack.addFirst(status);
								valueStack.addFirst(new LinkedHashMap());
								break;
							case Yytoken.TYPE_LEFT_SQUARE:
								status = S_IN_ARRAY;
								statusStack.addFirst(status);
								valueStack.addFirst(new ArrayList());
								break;
							default:
								status = S_IN_ERROR;
						}
						break;

					case S_IN_FINISHED_VALUE:
						if(token.type == Yytoken.TYPE_EOF)
							return valueStack.removeFirst();
						else
							throw new RuntimeException("Unexpected token: " + token);

					case S_IN_OBJECT:
						switch(token.type)
						{
							case Yytoken.TYPE_COMMA:
								break;
							case Yytoken.TYPE_VALUE:
								if(token.value instanceof String)
								{
									String key = (String)token.value;
									valueStack.addFirst(key);
									status = S_PASSED_PAIR_KEY;
									statusStack.addFirst(status);
								}
								else
								{
									status = S_IN_ERROR;
								}
								break;
							case Yytoken.TYPE_RIGHT_BRACE:
								if(valueStack.size() > 1)
								{
									statusStack.removeFirst();
									valueStack.removeFirst();
									status = peekStatus(statusStack);
								}
								else
								{
									status = S_IN_FINISHED_VALUE;
								}
								break;
							default:
								status = S_IN_ERROR;
								break;
						}
						break;

					case S_PASSED_PAIR_KEY:
						switch(token.type)
						{
							case Yytoken.TYPE_COLON:
								break;
							case Yytoken.TYPE_VALUE:
								statusStack.removeFirst();
								String key = (String)valueStack.removeFirst();
								Map parent = (Map)valueStack.getFirst();
								parent.put(key, token.value);
								status = peekStatus(statusStack);
								break;
							case Yytoken.TYPE_LEFT_SQUARE:
								statusStack.removeFirst();
								key = (String)valueStack.removeFirst();
								parent = (Map)valueStack.getFirst();
								List newArray = new ArrayList();
								parent.put(key, newArray);
								status = S_IN_ARRAY;
								statusStack.addFirst(status);
								valueStack.addFirst(newArray);
								break;
							case Yytoken.TYPE_LEFT_BRACE:
								statusStack.removeFirst();
								key = (String)valueStack.removeFirst();
								parent = (Map)valueStack.getFirst();
								Map newObject = new LinkedHashMap();
								parent.put(key, newObject);
								status = S_IN_OBJECT;
								statusStack.addFirst(status);
								valueStack.addFirst(newObject);
								break;
							default:
								status = S_IN_ERROR;
						}
						break;

					case S_IN_ARRAY:
						switch(token.type)
						{
							case Yytoken.TYPE_COMMA:
								break;
							case Yytoken.TYPE_VALUE:
								List val = (List)valueStack.getFirst();
								val.add(token.value);
								break;
							case Yytoken.TYPE_RIGHT_SQUARE:
								if(valueStack.size() > 1)
								{
									statusStack.removeFirst();
									valueStack.removeFirst();
									status = peekStatus(statusStack);
								}
								else
								{
									status = S_IN_FINISHED_VALUE;
								}
								break;
							case Yytoken.TYPE_LEFT_BRACE:
								val = (List)valueStack.getFirst();
								Map newObject = new LinkedHashMap();
								val.add(newObject);
								status = S_IN_OBJECT;
								statusStack.addFirst(status);
								valueStack.addFirst(newObject);
								break;
							case Yytoken.TYPE_LEFT_SQUARE:
								val = (List)valueStack.getFirst();
								List newArray = new ArrayList();
								val.add(newArray);
								status = S_IN_ARRAY;
								statusStack.addFirst(status);
								valueStack.addFirst(newArray);
								break;
							default:
								status = S_IN_ERROR;
						}
						break;
					case S_IN_ERROR:
						throw new RuntimeException("Unexpected token: " + token);
				}
				if(status == S_IN_ERROR)
				{
					throw new RuntimeException("Unexpected token: " + token);
				}
			} while(token.type != Yytoken.TYPE_EOF);

			throw new RuntimeException("Unexpected token: " + token);
		}

		private int peekStatus(LinkedList statusStack)
		{
			if(statusStack.size() == 0)
				return -1;
			return (Integer)statusStack.getFirst();
		}

		private void nextToken()
		  throws IOException
		{
			token = lexer.yylex();
			if(token == null)
				token = new Yytoken(Yytoken.TYPE_EOF, null);
		}
	}

	private static class Yytoken
	{
		public static final int TYPE_VALUE = 0;
		public static final int TYPE_LEFT_BRACE = 1;
		public static final int TYPE_RIGHT_BRACE = 2;
		public static final int TYPE_LEFT_SQUARE = 3;
		public static final int TYPE_RIGHT_SQUARE = 4;
		public static final int TYPE_COMMA = 5;
		public static final int TYPE_COLON = 6;
		public static final int TYPE_EOF = -1;

		public int type = 0;
		public Object value = null;

		public Yytoken(int type, Object value)
		{
			this.type = type;
			this.value = value;
		}
	}

	private static class Yylex
	{

		public static final int YYEOF = -1;

		private static final int ZZ_BUFFERSIZE = 16384;

		public static final int YYINITIAL = 0;
		public static final int STRING_BEGIN = 2;

		private static final int ZZ_LEXSTATE[] = {
		  0, 0, 1, 1
		};

		private static final String ZZ_CMAP_PACKED =
		  "\11\0\1\7\1\7\2\0\1\7\22\0\1\7\1\0\1\11\10\0" +
			 "\1\6\1\31\1\2\1\4\1\12\12\3\1\32\6\0\4\1\1\5" +
			 "\1\1\24\0\1\27\1\10\1\30\3\0\1\22\1\13\2\1\1\21" +
			 "\1\14\5\0\1\23\1\0\1\15\3\0\1\16\1\24\1\17\1\20" +
			 "\5\0\1\25\1\0\1\26\uff82\0";

		private static final char[] ZZ_CMAP = zzUnpackCMap(ZZ_CMAP_PACKED);

		private static final int[] ZZ_ACTION = zzUnpackAction();

		private static final String ZZ_ACTION_PACKED_0 =
		  "\2\0\2\1\1\2\1\3\1\4\3\1\1\5\1\6" +
			 "\1\7\1\10\1\11\1\12\1\13\1\14\1\15\5\0" +
			 "\1\14\1\16\1\17\1\20\1\21\1\22\1\23\1\24" +
			 "\1\0\1\25\1\0\1\25\4\0\1\26\1\27\2\0" +
			 "\1\30";

		private static int[] zzUnpackAction()
		{
			int[] result = new int[45];
			zzUnpackAction(ZZ_ACTION_PACKED_0, 0, result);
			return result;
		}

		private static int zzUnpackAction(String packed, int offset, int[] result)
		{
			int i = 0;
			int j = offset;
			int l = packed.length();
			while(i < l)
			{
				int count = packed.charAt(i++);
				int value = packed.charAt(i++);
				do { result[j++] = value; } while(--count > 0);
			}
			return j;
		}

		private static final int[] ZZ_ROWMAP = zzUnpackRowMap();

		private static final String ZZ_ROWMAP_PACKED_0 =
		  "\0\0\0\33\0\66\0\121\0\154\0\207\0\66\0\242" +
			 "\0\275\0\330\0\66\0\66\0\66\0\66\0\66\0\66" +
			 "\0\363\0\u010e\0\66\0\u0129\0\u0144\0\u015f\0\u017a\0\u0195" +
			 "\0\66\0\66\0\66\0\66\0\66\0\66\0\66\0\66" +
			 "\0\u01b0\0\u01cb\0\u01e6\0\u01e6\0\u0201\0\u021c\0\u0237\0\u0252" +
			 "\0\66\0\66\0\u026d\0\u0288\0\66";

		private static int[] zzUnpackRowMap()
		{
			int[] result = new int[45];
			zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, 0, result);
			return result;
		}

		private static int zzUnpackRowMap(String packed, int offset, int[] result)
		{
			int i = 0;
			int j = offset;
			int l = packed.length();
			while(i < l)
			{
				int high = packed.charAt(i++) << 16;
				result[j++] = high | packed.charAt(i++);
			}
			return j;
		}

		private static final int ZZ_TRANS[] = {
		  2, 2, 3, 4, 2, 2, 2, 5, 2, 6,
		  2, 2, 7, 8, 2, 9, 2, 2, 2, 2,
		  2, 10, 11, 12, 13, 14, 15, 16, 16, 16,
		  16, 16, 16, 16, 16, 17, 18, 16, 16, 16,
		  16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		  16, 16, 16, 16, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, 4, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, 4, 19, 20, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, 20, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, 5, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  21, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, 22, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  23, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, 16, 16, 16, 16, 16, 16, 16,
		  16, -1, -1, 16, 16, 16, 16, 16, 16, 16,
		  16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		  -1, -1, -1, -1, -1, -1, -1, -1, 24, 25,
		  26, 27, 28, 29, 30, 31, 32, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  33, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, 34, 35, -1, -1,
		  34, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  36, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, 37, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, 38, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, 39, -1, 39, -1, 39, -1, -1,
		  -1, -1, -1, 39, 39, -1, -1, -1, -1, 39,
		  39, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, 33, -1, 20, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, 20, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, 35,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, 38, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, 40,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, 41, -1, -1, -1, -1, -1,
		  -1, -1, -1, -1, -1, 42, -1, 42, -1, 42,
		  -1, -1, -1, -1, -1, 42, 42, -1, -1, -1,
		  -1, 42, 42, -1, -1, -1, -1, -1, -1, -1,
		  -1, -1, 43, -1, 43, -1, 43, -1, -1, -1,
		  -1, -1, 43, 43, -1, -1, -1, -1, 43, 43,
		  -1, -1, -1, -1, -1, -1, -1, -1, -1, 44,
		  -1, 44, -1, 44, -1, -1, -1, -1, -1, 44,
		  44, -1, -1, -1, -1, 44, 44, -1, -1, -1,
		  -1, -1, -1, -1, -1,
		};

		private static final int ZZ_UNKNOWN_ERROR = 0;
		private static final int ZZ_NO_MATCH = 1;
		private static final int ZZ_PUSHBACK_2BIG = 2;

		private static final String ZZ_ERROR_MSG[] = {
		  "Unkown internal scanner error",
		  "Error: could not match input",
		  "Error: pushback value was too large"
		};

		private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();

		private static final String ZZ_ATTRIBUTE_PACKED_0 =
		  "\2\0\1\11\3\1\1\11\3\1\6\11\2\1\1\11" +
			 "\5\0\10\11\1\0\1\1\1\0\1\1\4\0\2\11" +
			 "\2\0\1\11";

		private static int[] zzUnpackAttribute()
		{
			int[] result = new int[45];
			zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, 0, result);
			return result;
		}

		private static int zzUnpackAttribute(String packed, int offset, int[] result)
		{
			int i = 0;
			int j = offset;
			int l = packed.length();
			while(i < l)
			{
				int count = packed.charAt(i++);
				int value = packed.charAt(i++);
				do { result[j++] = value; } while(--count > 0);
			}
			return j;
		}

		private Reader zzReader;

		private int zzState;

		private int zzLexicalState = YYINITIAL;

		private char zzBuffer[] = new char[ZZ_BUFFERSIZE];

		private int zzMarkedPos;

		private int zzCurrentPos;

		private int zzStartRead;

		private int zzEndRead;

		private boolean zzAtEOF;

		private StringBuffer sb = new StringBuffer();

		Yylex(Reader in)
		{
			this.zzReader = in;
		}

		private static char[] zzUnpackCMap(String packed)
		{
			char[] map = new char[0x10000];
			int i = 0;
			int j = 0;
			while(i < 90)
			{
				int count = packed.charAt(i++);
				char value = packed.charAt(i++);
				do { map[j++] = value; } while(--count > 0);
			}
			return map;
		}

		private boolean zzRefill()
		  throws IOException
		{

			if(zzStartRead > 0)
			{
				System.arraycopy(zzBuffer, zzStartRead,
				  zzBuffer, 0,
				  zzEndRead - zzStartRead);

				zzEndRead -= zzStartRead;
				zzCurrentPos -= zzStartRead;
				zzMarkedPos -= zzStartRead;
				zzStartRead = 0;
			}

			if(zzCurrentPos >= zzBuffer.length)
			{

				char newBuffer[] = new char[zzCurrentPos*2];
				System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
				zzBuffer = newBuffer;
			}

			int numRead = zzReader.read(zzBuffer, zzEndRead,
			  zzBuffer.length - zzEndRead);

			if(numRead > 0)
			{
				zzEndRead += numRead;
				return false;
			}

			if(numRead == 0)
			{
				int c = zzReader.read();
				if(c == -1)
				{
					return true;
				}
				else
				{
					zzBuffer[zzEndRead++] = (char)c;
					return false;
				}
			}

			return true;
		}

		public final void yybegin(int newState)
		{
			zzLexicalState = newState;
		}

		public final String yytext()
		{
			return new String(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
		}

		public final char yycharat(int pos)
		{
			return zzBuffer[zzStartRead + pos];
		}

		public final int yylength()
		{
			return zzMarkedPos - zzStartRead;
		}

		private void zzScanError(int errorCode)
		{
			String message;
			try
			{
				message = ZZ_ERROR_MSG[errorCode];
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
			}

			throw new Error(message);
		}

		public void yypushback(int number)
		{
			if(number > yylength())
				zzScanError(ZZ_PUSHBACK_2BIG);

			zzMarkedPos -= number;
		}

		public Yytoken yylex()
		  throws IOException
		{
			int zzInput;
			int zzAction;

			int zzCurrentPosL;
			int zzMarkedPosL;
			int zzEndReadL = zzEndRead;
			char[] zzBufferL = zzBuffer;
			char[] zzCMapL = ZZ_CMAP;

			int[] zzTransL = ZZ_TRANS;
			int[] zzRowMapL = ZZ_ROWMAP;
			int[] zzAttrL = ZZ_ATTRIBUTE;

			while(true)
			{
				zzMarkedPosL = zzMarkedPos;
				zzAction = -1;
				zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;
				zzState = ZZ_LEXSTATE[zzLexicalState];

				zzForAction:
				{
					while(true)
					{

						if(zzCurrentPosL < zzEndReadL)
							zzInput = zzBufferL[zzCurrentPosL++];
						else if(zzAtEOF)
						{
							zzInput = YYEOF;
							break zzForAction;
						}
						else
						{

							zzCurrentPos = zzCurrentPosL;
							zzMarkedPos = zzMarkedPosL;
							boolean eof = zzRefill();

							zzCurrentPosL = zzCurrentPos;
							zzMarkedPosL = zzMarkedPos;
							zzBufferL = zzBuffer;
							zzEndReadL = zzEndRead;
							if(eof)
							{
								zzInput = YYEOF;
								break zzForAction;
							}
							else
							{
								zzInput = zzBufferL[zzCurrentPosL++];
							}
						}
						int zzNext = zzTransL[zzRowMapL[zzState] + zzCMapL[zzInput]];
						if(zzNext == -1) break zzForAction;
						zzState = zzNext;

						int zzAttributes = zzAttrL[zzState];
						if((zzAttributes & 1) == 1)
						{
							zzAction = zzState;
							zzMarkedPosL = zzCurrentPosL;
							if((zzAttributes & 8) == 8) break zzForAction;
						}
					}
				}

				zzMarkedPos = zzMarkedPosL;

				switch(zzAction < 0?zzAction:ZZ_ACTION[zzAction])
				{
					case 11:
					{
						sb.append(yytext());
					}
					case 25: break;
					case 4:
					{
						sb = null; sb = new StringBuffer(); yybegin(STRING_BEGIN);
					}
					case 26: break;
					case 16:
					{
						sb.append('\b');
					}
					case 27: break;
					case 6:
					{
						return new Yytoken(Yytoken.TYPE_RIGHT_BRACE, null);
					}
					case 28: break;
					case 23:
					{
						Boolean val = Boolean.valueOf(yytext());
						return new Yytoken(Yytoken.TYPE_VALUE, val);
					}
					case 29: break;
					case 22:
					{
						return new Yytoken(Yytoken.TYPE_VALUE, null);
					}
					case 30: break;
					case 13:
					{
						yybegin(YYINITIAL); return new Yytoken(Yytoken.TYPE_VALUE, sb.toString
					  ());
					}
					case 31: break;
					case 12:
					{
						sb.append('\\');
					}
					case 32: break;
					case 21:
					{
						Double val = Double.valueOf(yytext());
						return new Yytoken(Yytoken.TYPE_VALUE, val);
					}
					case 33: break;
					case 1:
					{
						throw new RuntimeException("Unexpected character: " +
						  new Character(yycharat(0)));
					}
					case 34: break;
					case 8:
					{
						return new Yytoken(Yytoken.TYPE_RIGHT_SQUARE, null);
					}
					case 35: break;
					case 19:
					{
						sb.append('\r');
					}
					case 36: break;
					case 15:
					{
						sb.append('/');
					}
					case 37: break;
					case 10:
					{
						return new Yytoken(Yytoken.TYPE_COLON, null);
					}
					case 38: break;
					case 14:
					{
						sb.append('"');
					}
					case 39: break;
					case 5:
					{
						return new Yytoken(Yytoken.TYPE_LEFT_BRACE, null);
					}
					case 40: break;
					case 17:
					{
						sb.append('\f');
					}
					case 41: break;
					case 24:
					{
						try
						{
							int ch = Integer.parseInt(yytext().substring(2), 16);
							sb.append((char)ch);
						}
						catch(Exception e)
						{
							throw new RuntimeException("Unexpected exception!");
						}
					}
					case 42: break;
					case 20:
					{
						sb.append('\t');
					}
					case 43: break;
					case 7:
					{
						return new Yytoken(Yytoken.TYPE_LEFT_SQUARE, null);
					}
					case 44: break;
					case 2:
					{
						Long val = Long.valueOf(yytext());
						return new Yytoken(Yytoken.TYPE_VALUE, val);
					}
					case 45: break;
					case 18:
					{
						sb.append('\n');
					}
					case 46: break;
					case 9:
					{
						return new Yytoken(Yytoken.TYPE_COMMA, null);
					}
					case 47: break;
					case 3:
					{
					}
					case 48: break;
					default:
						if(zzInput == YYEOF && zzStartRead == zzCurrentPos)
						{
							zzAtEOF = true;
							return null;
						}
						else
						{
							zzScanError(ZZ_NO_MATCH);
						}
				}
			}
		}
	}
}