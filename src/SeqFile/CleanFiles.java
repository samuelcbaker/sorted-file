package SeqFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CleanFiles {
    public static void main(String[] args) {
        File[] files = { new File("0.ord"), new File("1.ord"), new File("2.ord"), new File("3.ord"), new File("4.ord"),
                new File("5.ord"), new File("6.ord"), new File("7.ord") };

        for (int i = 0; i < 8; i++) {
            RandomAccessFile clean;
            try {
                clean = new RandomAccessFile(files[i], "rw");
                clean.setLength(0);
                clean.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}