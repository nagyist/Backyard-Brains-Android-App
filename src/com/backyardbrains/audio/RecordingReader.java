package com.backyardbrains.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.backyardbrains.BackyardBrainsMain;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class RecordingReader {

	public static final String TAG = RecordingReader.class.getCanonicalName();
	private BufferedInputStream bufferedStream = null;
	private File recordingFile;
	private ReadFromWavefile asyncReader;

	Context context;
	// ----------------------------------------------------------------------------------------
		public RecordingReader(String filePath){
			try {
				loadFile(new File(filePath));
			} catch (IOException e) {
				Log.e(TAG, "Couldn't load wav file ");
				e.printStackTrace();
			}
		}
	// ----------------------------------------------------------------------------------------
	public RecordingReader(String filePath, Context context){
		this.context = context.getApplicationContext();
		try {
			loadFile(new File(filePath));
		} catch (IOException e) {
			Log.e(TAG, "Couldn't load wav file ");
			e.printStackTrace();
		}
	}
	// ----------------------------------------------------------------------------------------
	public RecordingReader(File f,Context context){
		this.context = context.getApplicationContext();
		try {
			loadFile(f);
		} catch (IOException e) {
			Log.e(TAG, "Couldn't load wav file ");
			e.printStackTrace();
		}
	}
	
	// ----------------------------------------------------------------------------------------
	public void loadFile(File f) throws IOException{
		asyncReader = new ReadFromWavefile();
		recordingFile = f;
		if(recordingFile != null){
		if(recordingFile.exists()){
			try {
				bufferedStream = new BufferedInputStream(new FileInputStream(recordingFile));
				asyncReader.execute(bufferedStream);
				//byte [] buff = convertFromWave(bufferedStream);
//				byte [] orig = new byte [buff.length];
//				if(buff.length != orig.length){Log.d("RecordingReader","Different size buffers");}
//				for(int i = 0; i < buff.length && i < orig.length; i++){
//					if(buff[i] != orig[i]){
//						Log.d("RecordingReader", "samples not equal");					}
//				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}	
		}		
		}
	}
	// ----------------------------------------------------------------------------------------
	public byte [] getData(){return asyncReader.getData();}
	// ----------------------------------------------------------------------------------------
	public boolean isReady(){return asyncReader.isReady();}
	// ----------------------------------------------------------------------------------------
	private byte [] convertFromWave(BufferedInputStream in)throws IOException {
		/* RIFF header */
        readId(in, "RIFF");
        int numBytes = readInt(in) - 36;
        readId(in, "WAVE");

        /* fmt chunk */
        readId(in, "fmt ");
        if (16 != readInt(in)) throw new IOException("fmt chunk length not 16");
        int mFormat = readShort(in);
        int mNumChannels = readShort(in);
        int mSampleRate = readInt(in);
        int byteRate = readInt(in);
        short blockAlign = readShort(in);
        int mBitsPerSample = readShort(in);
        if (byteRate != mNumChannels * mSampleRate * mBitsPerSample / 8) {
            throw new IOException("fmt.ByteRate field inconsistent");
        }
        if (blockAlign != mNumChannels * mBitsPerSample / 8) {
            throw new IOException("fmt.BlockAlign field inconsistent");
        }

        /* data chunk */
        readId(in, "data");
        int mNumBytes = readInt(in)*2/mNumChannels/mBitsPerSample*2;

        byte [] buff = new byte [mNumBytes];
        
        int readSize = in.read(buff);
        if(readSize == -1) throw new IOException("wav data end before expected");
        if(readSize != mNumBytes) throw new IOException("wav data size differs from what header says");
        Log.d(TAG, "Successfully read file. numBytes " + mNumBytes + " format " + mFormat + " numChannels " + mNumChannels + " samplerate: " + mSampleRate + " byteRate: " + byteRate + " blockAlign: " + blockAlign + " bitsPerSample: "+ mBitsPerSample);
        return buff;
		
	}
	// ----------------------------------------------------------------------------------------
	private static void readId(InputStream in, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != in.read()) throw new IOException( id + " tag not present");
        }
    }
	// ----------------------------------------------------------------------------------------
	private static int readInt(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }
	// ----------------------------------------------------------------------------------------
	private static short readShort(InputStream in) throws IOException {
        return (short)(in.read() | (in.read() << 8));
    }
	// ----------------------------------------------------------------------------------------
	// ----------------------------------------------------------------------------------------
	private class ReadFromWavefile extends AsyncTask<BufferedInputStream, Void, Void> {

		private boolean bReady = false;
		private byte [] buffer = {};
		

		public boolean isReady(){return bReady;}
		public byte [] getData(){
				return buffer;
		}
		@Override
		protected Void doInBackground(BufferedInputStream... params) {
			for (BufferedInputStream f : params) {
				try {
					buffer =convertFromWave(f);
				} catch (IOException e) {
					Log.e(TAG, "Couldn't read wav file ");
					e.printStackTrace();
				}
			}
			Log.d(getClass().getCanonicalName(),"Finished reading " + recordingFile.getName());
			return null;
		}
		@Override
		protected void onPostExecute(Void v) {
			bReady = true;
			Log.d(TAG, "onPostExecute: bReady = true");
			Intent i = new Intent();
			i.setAction("BYBAudioFileRead");
			//i.putExtra(name, data);
			context.sendBroadcast(i);
		}
		@Override
		protected void onPreExecute() {
			bReady = false;
		}
	}
	
}
