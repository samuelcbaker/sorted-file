package SeqFile;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FixedSizeStudent implements Comparable{
    private static final int NAMESIZE = 30; 
    public static final int DATASIZE = 68; //string 20 caracteres(40 bytes) + 4 bytes int + 4 bytes float
    String name;
    int id;
    float finalGrade;

    public String getName(){
        return name;
    }

    public FixedSizeStudent(String nm, int id, float grd){
        name = nm;
        this.id = id;
        finalGrade = grd;
    }

    public void saveData(RandomAccessFile arq) throws IOException{
        StringBuilder finalName = new StringBuilder(this.name);
        finalName.setLength(NAMESIZE);
        arq.writeChars(finalName.toString());   
        arq.writeInt(id);
        arq.writeFloat(finalGrade);
    }

    public static FixedSizeStudent readData(RandomAccessFile arq) throws IOException{
        char name[] = new char[NAMESIZE];
        for (int i=0; i<NAMESIZE; i++) {
          name[i] = arq.readChar();
        }
        String n = new String(name);
        n = n.trim();
        int id = arq.readInt();
        float grade = arq.readFloat();
        FixedSizeStudent readStudent = new FixedSizeStudent(n, id, grade);
        return readStudent;
    }

    public String toString(){
        return id+" - "+name+" - Grade: "+finalGrade;
    }

    @Override
    public int compareTo(Object o) {

        if(this.id > ((FixedSizeStudent) o).id){
            return 1;
        } else if (this.id < ((FixedSizeStudent) o).id) {
            return -1;
        }
        
        return 0;
    }

}