package SeqFile;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IndexStudent implements Comparable{
    public static final int DATASIZE = 8; //string 20 caracteres(40 bytes) + 4 bytes int + 4 bytes float
    int pos;
    int id;

    public IndexStudent(int id, int pos){
        this.id = id;
        this.pos = pos;
    }

    public void saveData(RandomAccessFile arq) throws IOException{
        arq.writeInt(pos);
        arq.writeInt(id);
    }

    public static IndexStudent readData(RandomAccessFile arq) throws IOException{
        int pos = arq.readInt();
        int id = arq.readInt();
        IndexStudent readIndex = new IndexStudent(id, pos);
        return readIndex;
    }

    @Override
    public int compareTo(Object o) {

        if(this.id > ((IndexStudent) o).id){
            return 1;
        } else if (this.id < ((IndexStudent) o).id) {
            return -1;
        }
        
        return 0;
    }

}