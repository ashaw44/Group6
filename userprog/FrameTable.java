package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;
import java.util.ArrayList;

class FrameTable {
    private Lock lock;
    private LinkedList<Integer> freeFrames;

    public FrameTable() {
        lock = new Lock();

        freeFrames = new LinkedList();
        for(int i = 0; i < Processor.getNumPhysPages(); i++) {
            freeFrames.add(i);
        }
    }

    public ArrayList requestFrames(int numFrames) {
        lock.acquire();
        if(numFrames > freeFrames.size()) {
            lock.release();
            return null;
        }

        ArrayList frames = new ArrayList(numFrames);
		
		for(int i = 0; i < numFrames; i++) {
            frames.add(freeFrames.remove());
        }

        lock.release();
        return frames;
    }

    public void releaseFrame(int frameNum) {
        lock.acquire();
        freeFrames.add(frameNum);
        lock.release();
    }
}