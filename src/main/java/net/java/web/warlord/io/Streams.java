package net.java.web.warlord.io;

/* Java */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/* Warlord */

import net.java.web.warlord.EX;


/**
 * Collection of input-output streams, buffers
 * and other related structures.
 *
 * @author anton.baukin@gmail.com
 */
public class Streams
{
	/* Shared Pool of Buffers */

	public static final ByteBuffers BUFFERS =
	  new ByteBuffers();

	/**
	 * Weak buffer of 512 byte arrays.
	 */
	public static final class ByteBuffers
	{
		/* Byte Buffers */

		public byte[] get()
		{
			final WeakReference<Queue<byte[]>> wr = this.pool.get();
			final Queue<byte[]> q = (wr == null)?(null):(wr.get());

			//?: {queue does not exist}
			if(q == null)
				return new byte[512];

			//~: poll the queue
			final byte[] b = q.poll();

			//~: return | create
			return (b != null)?(b):(new byte[512]);
		}

		public void   free(Collection<byte[]> bufs)
		{
			//?: {nothing to do}
			if((bufs == null) || bufs.isEmpty())
				return;

			//~: existing pool
			final WeakReference<Queue<byte[]>> wr = this.pool.get();
			final Queue<byte[]> q = (wr == null)?(null):(wr.get());

			if(q != null) //?: {queue does exist}
			{
				//~: add valid buffers
				for(byte[] buf : bufs)
					if((buf != null) && (buf.length == 512))
						q.offer(buf);

				return;
			}

			//~: create own pool
			final Queue<byte[]> q2 = new ConcurrentLinkedQueue<>();
			final WeakReference<Queue<byte[]>> wr2 =
			  new WeakReference<>(q2);

			//?: {swapped it not to the field} waste the buffers
			if(!this.pool.compareAndSet(wr, wr2))
				return;

			//~: add valid buffers
			for(byte[] buf : bufs)
				if((buf != null) && (buf.length == 512))
					q2.offer(buf);
		}

		public void   free(byte[] buf)
		{
			if(buf == null || buf.length != 512)
				throw new IllegalArgumentException();
			this.free(Collections.singleton(buf));
		}

		private final AtomicReference<WeakReference<Queue<byte[]>>>
		  pool = new AtomicReference<>();
	}


	/* Characters Bytes */

	/**
	 * Takes a character sequence, splits it in
	 * multiple small buffers (on demand) and
	 * provides them as input stream.
	 *
	 * Replaces String.getBytes("UTF-8") not
	 * producing large byte buffers.
	 */
	public static final class CharBytes extends InputStream
	{
		public CharBytes(CharSequence string)
		{
			this(256, string);
		}

		public CharBytes(int buffer, CharSequence string)
		{
			this.buffer = buffer;
			this.string = EX.assertn(string);
			EX.assertx(buffer > 0);

			try
			{
				this.o = new OutputStreamWriter(w, "UTF-8");
			}
			catch(Exception e)
			{
				throw EX.wrap(e);
			}
		}

		public final CharSequence string;


		/* Input Stream */

		public int  read()
		  throws IOException
		{
			if((b == null) || (j == b.length))
			{
				b = next();

				if(b == null)
					return -1;
			}

			return b[j++] & 0xFF;
		}

		public int  read(byte[] buf, int o, int l)
		  throws IOException
		{
			int s = 0;

			while(l > 0)
			{
				if((b == null) || (j == b.length))
				{
					b = next();

					if(b == null)
						break;
				}

				//~: the remaining length
				int x = Math.min(l, b.length - j);

				System.arraycopy(b, j, buf, o, x);
				j += x; o += x; l -= x; s += x;
			}

			if(s > 0)
				return s;

			if((b == null) || (j == b.length))
				b = next();

			return (b == null)?(-1):(0);
		}

		public void close()
		{
			j = string.length();
			b = null;
		}


		/* private: local buffer */

		/**
		 * Converts the following sequence of characters
		 * with support for surrogate pairs.
		 */
		private byte[] next()
		  throws IOException
		{
			final int sl = string.length();

			//?: {no characters}
			if(i >= sl)
				return null;

			//~: length to copy
			int l = Math.min(buffer, sl - i);

			//~: substring of the interest
			String x = string.subSequence(i, i + l).toString();

			//~: write it
			o.write(x);
			i += l;
			o.flush();

			j = 0; //<-- start the new buffer
			return w.reset();
		}

		/**
		 * Current index in the string.
		 */
		private int i;

		/**
		 * Current index in the buffer.
		 */
		private int j;

		/**
		 * Current buffer.
		 */
		private byte[] b;

		/**
		 * Approximated size of the bytes buffer.
		 * Used primary for the testing.
		 */
		private final int buffer;

		/**
		 * Wrapper of the encoding stream
		 */
		private final WrappingBytes w = new WrappingBytes();

		private final OutputStreamWriter o;


		/* Wrapping Bytes */

		private class WrappingBytes extends OutputStream
		{
			public void   write(int b)
			  throws IOException
			{
				bos.write(b);
				length++;
			}

			public void   write(byte[] b, int off, int len)
			  throws IOException
			{
				bos.write(b, off, len);
				length += len;
			}

			public byte[] reset()
			{
				byte[] a = bos.toByteArray();

				bos = new ByteArrayOutputStream(buffer * 2);
				length = 0;

				return a;
			}

			private ByteArrayOutputStream bos =
			  new ByteArrayOutputStream(buffer * 2);

			private int length;
		}
	}


	/* Chunk Bytes Stream */

	/**
	 * Output stream that uses shared buffers and
	 * allows simultaneous reading of written bytes.
	 * This implementation is not thread-safe!
	 */
	public static final class BytesStream extends OutputStream
	{
		/* public: BytesStream interface */

		/**
		 * Copies the bytes written to the stream given.
		 */
		public void        copy(OutputStream stream)
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");

			if(buffers.isEmpty())
				return;

			byte[] last = buffers.get(buffers.size() - 1);
			for(byte[] buf : buffers)
				stream.write(buf, 0, (buf == last)?(position):(buf.length));
		}

		/**
		 * Copies the bytes to the array given and returns
		 * the number of bytes actually copied.
		 *
		 * @param off  the offset within the argument array.
		 */
		public int         copy(byte[] a, int off, int len)
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");

			if(buffers.isEmpty())
				return 0;

			int    res  = 0;
			byte[] last = buffers.get(buffers.size() - 1);

			for(byte[] buf : buffers)
			{
				int sz = (buf == last)?(position):(buf.length);
				if(sz > len) sz = len;

				System.arraycopy(buf, 0, a, off, sz);
				off += sz; len -= sz; res += sz;

				if(len == 0) break;
			}

			return res;
		}

		/**
		 * Returns a copy of the bytes written.
		 */
		public byte[]      bytes()
		  throws IOException
		{
			byte[] res = new byte[(int) length()];
			int    csz = copy(res, 0, res.length);

			EX.assertx(res.length == csz,
			  "Error in BytesStream.copy(bytes) implementation!"
			);

			return res;
		}

		/**
		 * Writes all the bytes from the stream given.
		 * The stream is not closed in this call.
		 */
		public void        write(InputStream stream)
		  throws IOException
		{
			byte[] buf = BUFFERS.get();
			int    sz;

			try
			{
				while((sz = stream.read(buf)) > 0)
					write(buf, 0, sz);
			}
			finally
			{
				BUFFERS.free(buf);
			}
		}

		public long        length()
		{
			return this.length;
		}

		public void        digest(MessageDigest md)
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");

			if(buffers.isEmpty())
				return;

			byte[] last = buffers.get(buffers.size() - 1);
			for(byte[] buf : buffers)
				md.update(buf, 0, (buf == last)?(position):(buf.length));
		}

		public InputStream inputStream()
		{
			return new Stream();
		}

		public boolean     isNotCloseNext()
		{
			return notCloseNext;
		}

		public BytesStream setNotCloseNext(boolean notCloseNext)
		{
			this.notCloseNext = notCloseNext;
			return this;
		}

		public boolean     isNotClose()
		{
			return notClose;
		}

		public BytesStream setNotClose(boolean notClose)
		{
			this.notClose = notClose;
			return this;
		}


		/* public: OutputStream interface */

		public void write(int b)
		  throws IOException
		{
			if(byte1 == null)
				byte1 = new byte[1];
			byte1[0] = (byte) b;

			this.write(byte1, 0, 1);
		}

		public void write(byte[] b, int off, int len)
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");

			while(len > 0)
			{
				//?: {no a buffer}
				if(buffers.isEmpty())
				{
					buffers.add(BUFFERS.get());
					continue;
				}

				byte[] x = buffers.get(buffers.size() - 1);
				int    s = x.length - position;

				//?: {has no free space in the current buffer}
				if(s == 0)
				{
					buffers.add(BUFFERS.get());
					position = 0;
					continue;
				}

				//?: {restrict free space to the length left}
				if(s > len) s = len;

				System.arraycopy(b, off, x, position, s);
				off += s; len -= s; position += s;
				this.length += s;
			}
		}

		public void erase()
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");

			BUFFERS.free(buffers);
			buffers.clear();

			length = position = 0;
		}

		public void flush()
		  throws IOException
		{
			if(buffers == null)
				throw new IOException("ByteStream is closed!");
		}

		public void close()
		{
			if((buffers == null) | notClose)
				return;

			if(notCloseNext)
			{
				notCloseNext = false;
				return;
			}

			BUFFERS.free(buffers);
			buffers = null;
		}

		public void closeAlways()
		{
			this.notClose = this.notCloseNext = false;
			this.close();
		}


		/* Input Stream */

		private class Stream extends InputStream
		{
			/* public: InputStream interface */

			public int     read()
			  throws IOException
			{
				if(byte1 == null)
					byte1 = new byte[1];

				int x = this.read(byte1, 0, 1);
				return (x <= 0)?(-1):(byte1[0] & 0xFF);
			}

			public int     read(byte[] b, int off, int len)
			  throws IOException
			{
				EX.assertn(b);
				if((off < 0) | (len < 0) | (len > b.length - off))
					throw new IndexOutOfBoundsException();

				if(buffers == null)
					throw new IOException("ByteStream is closed!");
				if(bufind  == -1)
					throw new IOException("Input Stream of ByteStream is closed!");

				if(buffers.isEmpty())
					return -1;

				int got = 0;

				while(len != 0)
				{
					byte[] buf = buffers.get(bufind);
					int    sz;

					//?: {it is the current buffer}
					if(bufind == buffers.size() - 1)
					{
						if(bufpos == position)
							break;

						sz = position - bufpos;
					}
					//!: it is one of the fully filled buffers
					else if(bufpos == buf.length)
					{
						bufind++; bufpos = 0;
						continue;
					}
					else
						sz = buf.length - bufpos;

					if(sz > len) sz = len;
					System.arraycopy(buf, bufpos, b, off, sz);
					bufpos += sz; off += sz; len -= sz; got += sz;
				}

				return (got == 0)?(-1):(got);
			}

			public void    close()
			  throws IOException
			{
				this.bufind = -1;
			}

			public boolean markSupported()
			{
				return false;
			}


			/* private: read position */

			private int    bufind;
			private int    bufpos;
			private byte[] byte1;
		}


		/* private: list of buffers */

		private ArrayList<byte[]> buffers =
		  new ArrayList<>(16);

		/**
		 * The position within the last
		 * buffer of the list.
		 */
		private int     position;

		private long    length;
		private boolean notCloseNext;
		private boolean notClose;
		private byte[]  byte1;
	}


	/* Base 64 Decoder */

	/**
	 * Decodes Base64-encoded text back into the bytes.
	 * The origination of this class comes from Base64
	 * Java project taken from SourceForge.
	 *
	 * @author anton.baukin@gmail.com
	 * @author rob@iharder.net (Robert Harder)
	 */
	public static final class Base64Decoder extends InputStream
	{
		/* public: constructors */

		/**
		 * Creates the decoder with the standard input (decode)
		 * buffer to store 1024 3-byte triples.
		 */
		public Base64Decoder(InputStream stream)
		{
			this(stream, 1024*3);
		}

		public Base64Decoder(InputStream stream, int buf)
		{
			EX.assertx(buf > 0);
			this.stream = stream;
			this.buffer = new byte[buf];
		}


		/* public: InputStream interface */

		public int read()
		  throws IOException
		{
			//?: {has valid bytes in three array} take the current one
			if(to != ts)
				return (three >>> ((to += 8) - 8)) & 0xFF;
			to = ts = 0;

			int f = four;  //<-- stack access to four-bytes
			int p = 0;     //<-- position in the four-bytes (in BITS!)

			while(true)
			{
				//?: {has valid bytes in three array} take the current one
				if(to != ts)
					return (three >>> ((to += 8) - 8)) & 0xFF;

				//?: {need to upload the buffer}
				if(boffs >= bsize)
				{
					upload_to_buffer();

					//?: {has no more bytes in the input stream} stop reading
					if(bsize == 0) return -1;
				}

				//pickup the next character
				byte o = buffer[boffs++];   //<-- signed octet (character)
				byte d;

				//?: {is not a meaningful value} skip it
				if(((o >> 7) != 0) || ((d = DECODABET[o]) <= WS))
					continue;

				f |= ((d & 0xFF) << p); //<-- append to the decode array
				p += 8;

				//NOTE: that we waste the ending characters of the stream is
				//  being closed if they do not form a valid 4-character pack.

				//?: {has all the four bytes set} decode them
				if(p == 32)
				{
					four = f;
					decode4to3();
					four = f = p = 0;
				}
			}
		}

		public int read(byte dst[], int off, int len)
		  throws IOException
		{
			byte[] buf = buffer;
			int    pos = off;    //<-- the position in 'dst'

			int    p   = 0;      //<-- position in the four-bytes (in BITS!)
			int    f   = four;   //<-+
			int    t   = three;  //<-- stack access optimizations

			while(len > 0)
			{
				//?: {has valid bytes in three array} take the current one
				if(to != ts)
				{
					dst[pos++] = (byte)(t >>> to);
					to += 8; len--;
					continue;
				}
				to = ts = 0;

				//?: {need to upload the buffer}
				if(boffs >= bsize)
				{
					upload_to_buffer();

					//NOTE: that we waste the ending characters of the stream
					//  if they do not form a full 4-character pack.

					//?: {has no more bytes in the input stream} stop reading
					if(bsize == 0)
						//?: {had bytes} return the number
						return (pos != off)?(pos - off):(-1);
				}

				//pickup the next character
				byte o = buf[boffs++];      //<-- signed octet (character)
				byte d;

				//?: {is not a meaningful value} skip it
				if(((o >> 7) != 0) || ((d = DECODABET[o]) <= WS))
					continue;

				f |= ((d & 0xFF) << p); //<-- append to the decode array
				p += 8;

				//NOTE: that we waste the ending characters of the stream is
				//  being closed if they do not form a valid 4-character pack.

				//?: {has all the four bytes set} decode them
				if(p == 32)
				{
					four = f;
					decode4to3();
					t    = three;
					four = f = p = 0;
				}
			}

			//HINT: here we do not return -1 in the case when the input
			// buffer was smaller than 4 characters.

			return pos - off;
		}

		public void close()
		  throws IOException
		{
			stream.close();
		}


		/* private: streaming procedures */

		/**
		 * Uploads bytes to the buffer from the input stream. Bytes of the buffer
		 * starting from <tt>valid_offset</tt> are moved to the beginning of the
		 * buffer.
		 */
		private void upload_to_buffer()
		  throws IOException
		{
			//NOTE: that here we always have (offset == bsize)

			boffs = 0;
			bsize = stream.read(buffer);
			if(bsize < 0) bsize = 0;
		}

		/* private: decoding procedure */

		/**
		 * Translates a Base64 value to either its 6-bit reconstruction value
		 * or a negative number indicating some other meaning.
		 */
		private final static byte[] DECODABET =
		{
		  // Decimal  0 -  8
		  -9, -9, -9, -9, -9, -9, -9, -9, -9,

		  // Whitespace: Tab and Linefeed
		  -5, -5,

		  // Decimal 11 - 12
		  -9, -9,                                      // Decimal 11 - 12

		  // Whitespace: Carriage Return
		  -5,

		  // Decimal 14 - 26
		  -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,

		  // Decimal 27 - 31
		  -9, -9, -9, -9, -9,

		  // Whitespace: Space
		  -5,

		  // Decimal 33 - 42
		  -9, -9, -9, -9, -9, -9, -9, -9, -9, -9,

		  // Plus sign at decimal 43
		  62,

		  // Decimal 44 - 46
		  -9, -9, -9,

		  // Slash at decimal 47
		  63,

		  // Numbers zero through nine
		  52, 53, 54, 55, 56, 57, 58, 59, 60, 61,

		  // Decimal 58 - 60
		  -9, -9, -9,

		  // Equals sign at decimal 61
		  -1,

		  // Decimal 62 - 64
		  -9, -9, -9,

		  // Letters 'A' through 'N'
		  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
		  // Letters 'O' through 'Z'
		  14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,

		  // Decimal 91 - 96
		  -9, -9, -9, -9, -9, -9,

		  // Letters 'a' through 'm'
		  26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
		  // Letters 'n' through 'z'
		  39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,

		  -9, -9, -9, -9
		};

		/**
		 * Indicates white space in encoding.
		 */
		private final static byte WS = -5;

		/**
		 * Indicates white space in encoding.
		 */
		private final static int  EQ = 0xFF;

		/**
		 * Converts four Base-64 characters into up to 3 bytes.
		 */
		private void decode4to3()
		{
			int f = four;  //<-- stack access optimizations
			int t;
			int a;
			int b;

			//?: {????} has all three bytes
			if((f & 0xFF000000) != (EQ << 24))
			{
				a  = ((f             ) >>> 24);
				b  = ((f & 0x0000FF00) <<   4);
				a |= ((f & 0x00FF0000) >>> 10);
				b |= ((f & 0x000000FF) <<  18);
				t  = a | b;

				three = ((a & 0xFF) << 16) | (t & 0xFF00) | (t >>> 16);
				ts    = 24;
				return;
			}

			//?: {???=} has two bytes
			if((f & 0x00FF0000) != (EQ << 16))
			{
				a  = ((f & 0x0000FF00) <<   4);
				b  = ((f & 0x00FF0000) >>> 10);
				a |= ((f & 0x000000FF) <<  18);
				t  = b | a;

				three = (t >>> 16) | (t & 0xFF00);
				ts    = 16;
				return;
			}

			//?: {??==} has one byte
			if((f & 0x0000FF00) != (EQ << 8))
			{
				t = ((f & 0x000000FF) <<  18) |
				    ((f & 0x0000FF00) <<   4);

				three = (t >>> 16);
				ts    = 8;
				return;
			}

			//!: there is no bytes
			ts = 0;
		}

		/* private: streaming data */

		private InputStream  stream;
		private byte[]       buffer;
		private int          bsize;  //<-- the number of valid bytes in the buffer
		private int          boffs;  //<-- the position in the buffer

		/* private: decoding data */

		private int four;   //<-- 'buffer' of four income characters
		private int three;  //<-- 'buffer' of three outcome bytes
		private int to;     //<-- offset in 'three' array (in BITS!)
		private int ts;     //<-- the number of valid BITS in 'three' array
	}


	/**
	 * Encodes byte stream into stream of BASE64 characters.
	 * The origination of this class comes from Base64
	 * Java project taken from SourceForge.
	 *
	 * @author anton.baukin@gmail.com
	 * @author rob@iharder.net (Robert Harder)
	 */
	public static final class Base64Encoder extends OutputStream
	{
	/* public: constructors */

		/**
		 * Creates the buffer for standard 77-character lines.
		 * Each line has 76 characters for 19 income bytes and
		 * ends with '\n' separator.
		 *
		 * The number of lines in the buffer is 16. It's total
		 * length is 1232 characters (2464 bytes).
		 */
		public Base64Encoder(OutputStream stream)
		{
			this(stream, 19, 16);
		}

		public Base64Encoder(OutputStream stream, int bsize)
		{
			this(stream, 19, bsize);
		}

		/**
		 * @param lsize
		 *
		 *   defines the factor maximum length of the output string.
		 *   Real maximum length equals to <tt>4*lsize</tt> because
		 *   each 3 income bytes produces 4 characters. After each
		 *   <tt>4*lsize</tt> characters '\n' character is written.
		 *
		 *   The argument may be zero. In this case no '\n' characters
		 *   are written. By default is is equals to 19 (76 characters).
		 *
		 * @param bsize
		 *
		 *   defines the number of lines in the internal buffer used
		 *   while encoding. When the buffer fully filled, it is flashed
		 *   to the output.
		 *
		 *   The length of the buffer (in characters) equals to
		 *   <tt>(4*lsize + 1)*bsize</tt>. One character is reserved for
		 *   '\n'. When '\n' are not inserted, the length of the buffer
		 *   is  equal to <tt>4*bsize</tt> not to break character stream
		 *   in the middle of 3-byte triple.
		 */
		public Base64Encoder(OutputStream stream, int lsize, int bsize)
		{
			EX.assertx(lsize >= 0);
			EX.assertx(bsize >= 0);

			this.stream = stream;
			this.lmax   = (lsize != 0)?(4*lsize):(-1);
			this.buffer = new byte[(lsize != 0)?((4*lsize + 1)*bsize):(4*bsize)];
		}


		/* public: OutputStream interface */

		public void write(int bt)
		  throws IOException
		{
			//?: {has space in three-buffer} add byte & return
			if(ts != 24)
			{
				three |= ((bt & 0xFF) << ts);
				ts += 8;
				return;
			}

			// flush three-buffer

			//?: {streaming buffer is full} flush it
			if(bsize == buffer.length)
			{
				stream.write(buffer);
				lsize = bsize = 0;
			}

			//append the bytes
			encode3to4();
			lsize += 4;
			bsize += 4;

			//?: {the line is full} flush it
			if(lsize == lmax)
			{
				buffer[bsize++] = '\n';
				lsize = 0;
			}

			//~flush three-buffer

			//append byte
			three = bt & 0xFF;
			ts = 8;
		}

		public void write(byte b[], int off, int len)
		  throws IOException
		{
			int bt;

			for(int o = off; (len != 0); o++, len--)
			{
				bt = b[o];

				//?: {has space in three-buffer} add byte & return
				if(ts != 24)
				{
					three |= ((bt & 0xFF) << ts);
					ts += 8;
					continue;
				}

				// flush three-buffer

				//?: {streaming buffer is full} flush it
				if(bsize == buffer.length)
				{
					stream.write(buffer);
					lsize = bsize = 0;
				}

				//append the bytes
				encode3to4();
				lsize += 4;
				bsize += 4;

				//?: {the line is full} flush it
				if(lsize == lmax)
				{
					buffer[bsize++] = '\n';
					lsize = 0;
				}

				//~flush three-buffer

				//append byte
				three = bt & 0xFF;
				ts = 8;
			}
		}

		public void close()
		  throws IOException
		{
			flushBeforeClose();
			stream.close();
		}

		/**
		 * Flushes to the underlying stream all the lines
		 * that are filled fully. Hence the last line of the
		 * buffer may be not written.
		 */
		public void flush()
		  throws IOException
		{
			//?: {has new FULL bytes in three-buffer} flush them
			if(ts == 24)
			{
				//?: {streaming buffer is full} flush it
				if(bsize == buffer.length)
				{
					stream.write(buffer);
					lsize = bsize = 0;
				}

				//append the bytes of the three-array
				encode3to4();
				lsize += 4;
				bsize += 4;
				ts = three = 0;

				//?: {the line is full} terminate it
				if(lsize == lmax)
				{
					buffer[bsize++] = '\n';
					lsize = 0;
				}
			}

			//?: {not writing '\n'} write the whole buffer
			if(lmax == -1)
			{
				stream.write(buffer, 0, bsize);
				lsize = bsize = 0; //<-- 'lsize' means nothing in this case
			}
			//!: write to the buffer all the lines except the current one
			else if(bsize != lsize) //<-- the same as: (bsize > lsize)
			{
				stream.write(buffer, 0, bsize - lsize);
				if(lsize != 0)
					System.arraycopy(buffer, bsize - lsize, buffer, 0, lsize);
				bsize = lsize;
			}
		}

		/**
		 * Flushes the stream adding the padding characters
		 * to fulfill 4-characters terminal block. In the same
		 * manner the stream is flushed before closing.
		 */
		public void flushFully()
		  throws IOException
		{
			flushBeforeClose();
		}


		/* private: encoding procedure */

		private void encode3to4()
		{
			byte[] b = buffer;
			int    t = three;
			int    o = bsize;

			//?: {have all three income bytes}
			if(ts == 24)
			{
				//swap the first and the third bytes
				t = (t & 0xFF00) | ((t & 0xFF) << 16) | ((t >>> 16) & 0xFF);

				b[o    ] = ABC[(t >>> 18)       ];
				b[o + 1] = ABC[(t >>> 12) & 0x3F];
				b[o + 2] = ABC[(t >>>  6) & 0x3F];
				b[o + 3] = ABC[(t       ) & 0x3F];

				return;
			}

			//?: {have two income bytes}
			if(ts == 16)
			{
				//swap the first and the third bytes
				t = (t & 0xFF00) | ((t & 0xFF) << 16);

				b[o    ] = ABC[(t >>> 18)       ];
				b[o + 1] = ABC[(t >>> 12) & 0x3F];
				b[o + 2] = ABC[(t >>>  6) & 0x3F];
				b[o + 3] = EQ;

				return;
			}

			//?: {have one income byte}
			if(ts == 8)
			{
				//swap the first and the third bytes
				t = (t & 0xFF) << 16;

				b[o    ] = ABC[(t >>> 18)       ];
				b[o + 1] = ABC[(t >>> 12) & 0x3F];
				b[o + 2] = EQ;
				b[o + 3] = EQ;

				return;
			}

			//!: has no bytes
			b[o] = b[o + 1] = b[o + 2] = b[o + 3] = EQ;
		}

		private void flushBeforeClose()
		  throws IOException
		{
			//?: {has new bytes in three-buffer} flush them
			if(ts != 0)
			{
				//?: {streaming buffer is full} flush it
				if(bsize == buffer.length)
				{
					stream.write(buffer);
					lsize = bsize = 0;
				}

				//append the bytes of the three-array
				encode3to4();
				lsize += 4;
				bsize += 4;
				ts = three = 0;

				//?: {the line is full} terminate it
				if(lsize == lmax)
				{
					buffer[bsize++] = '\n';
					lsize = 0;
				}
			}

			//!: write the buffer as it is
			if(bsize != 0)
			{
				stream.write(buffer, 0, bsize);
				lsize = bsize = 0;
			}
		}


		/* private: encoding alphabet */

		private static final byte EQ = (int)('=');

		private static final byte[] ABC;
		private static final String ABCS =
		  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

		static
		{
			try
			{
				ABC = ABCS.getBytes("ISO-8859-1");
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}


		/* private: streaming data */

		private OutputStream stream;
		private byte[]       buffer;
		private int          lmax;   //<-- the maximum length of the text line (%4 == 0)
		private int          bsize;  //<-- the number of valid bytes in the buffer
		private int          lsize;  //<-- the size of the current text line

	/* private: encoding data */

		private int          three;  //<-- 'buffer' of three income bytes
		private int          ts;     //<-- the number of valid BITS in three buffer
	}
}