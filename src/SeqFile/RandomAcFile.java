package SeqFile;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RandomAcFile {

    static List<IndexStudent> indexStudents = new ArrayList<>();

    static final int BLOCK_SIZE = 10;
    static final int FILE_SOURCES = 4;

    // Cria um novo aluno (registro de tamanho fixo)
    public static FixedSizeStudent createStudent(Scanner input) {
        System.out.print("Name: ");
        String name = input.nextLine();

        System.out.print("Id: ");
        int id = input.nextInt();

        System.out.print("Grade: ");
        float grade = input.nextFloat();
        input.nextLine();
        return new FixedSizeStudent(name, id, grade);
    }

    // método para iterar e criar diversos estudantes. Utiliza writeObject para
    // persistir o dado no disco, de forma sequencial
    public static void createFile(Scanner input, File file) throws IOException {
        RandomAccessFile studentData = new RandomAccessFile(file, "rw");
        studentData.seek(file.length());

        char oneMore = 'y';
        FixedSizeStudent auxStudent;

        while (oneMore == 'y') {
            auxStudent = createStudent(input); // cria o estudante (método acima)
            auxStudent.saveData(studentData);
            System.out.print("One more (y/n)? ");
            oneMore = input.nextLine().charAt(0); // para terminar, 'y/n' para criar novo aluno(leitura do teclado)
        }
        studentData.close();
    }

    public static FixedSizeStudent loadData(File file, int pos) throws IOException, ClassNotFoundException {

        RandomAccessFile studentData = new RandomAccessFile(file, "r");
        studentData.seek(pos * FixedSizeStudent.DATASIZE);
        FixedSizeStudent student = null;
        try {
            student = FixedSizeStudent.readData(studentData);

        } catch (EOFException e) { // para quando terminar os dados do arquivo
            student = null;
        }

        studentData.close();
        return student;
    }

    // metodo para criar arquivo binario a partir do arquivo de texto
    public static void textFileToBinaryFile(File text, File binary) throws IOException {
        // acesso de leitura e escrita ao arquivo
        RandomAccessFile studentData = new RandomAccessFile(binary, "rw");
        Scanner reader = new Scanner(text);

        while (reader.hasNextLine()) {
            String s = reader.nextLine();

            // separa a linha lida dentro de um vetor separando pelo ';'
            String[] v = s.split(";");
            if (v.length == 0) {
                break;
            }

            int id = Integer.parseInt(v[0]);
            String name = v[1];
            float grade = Float.parseFloat(v[2].replace(",", "."));

            FixedSizeStudent auxStudent = new FixedSizeStudent(name, id, grade);

            auxStudent.saveData(studentData);
        }
        reader.close();
        studentData.close();
    }

    // metodo para criar o o indice do aluno
    public static void createIndexFile(File file, File indexFile) throws IOException {
        RandomAccessFile studentData = new RandomAccessFile(file, "r");

        RandomAccessFile studentIndex = new RandomAccessFile(indexFile, "rw");

        List<FixedSizeStudent> students = new ArrayList<>();

        // calculo do numero de alunos
        double numberOfStudents = file.length() / FixedSizeStudent.DATASIZE;

        for (int i = 0; i < numberOfStudents; i++) {

            int pos = i * FixedSizeStudent.DATASIZE;

            studentData.seek(pos);

            FixedSizeStudent student = null;
            try {
                student = FixedSizeStudent.readData(studentData);
                students.add(student);
            } catch (EOFException e) {
                student = null;
            }

        }

        // criacao dos indices do aluno
        for (int i = 0; i < numberOfStudents; i++) {

            int pos = i * FixedSizeStudent.DATASIZE;
            indexStudents.add(new IndexStudent(students.get(i).id, pos));
        }

        // ordenacao crescente pelo id do aluno
        indexStudents.sort((a,b) -> a.compareTo(b));

        indexStudents.forEach((i) -> {
            try {
                i.saveData(studentIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        studentData.close();
        studentIndex.close();
    }

    public static FixedSizeStudent findStudentById(int id, File file) throws IOException, ClassNotFoundException {
        RandomAccessFile studentData = new RandomAccessFile(file, "r");

        List<IndexStudent> indexes = indexStudents.stream().filter((student) -> student.id == id).collect(Collectors.toList());
        if(indexes.isEmpty()){
            return null;
        }
        IndexStudent indexStudent = indexes.get(0);

        studentData.seek(indexStudent.pos);
        FixedSizeStudent student = null;
        try {
            student = FixedSizeStudent.readData(studentData);

        } catch (EOFException e) { // para quando terminar os dados do arquivo
            student = null;
        }

        studentData.close();
        return student;
    }

    //Carregando indice para a memoria principal
    public static void loadIndex(File file) throws IOException {
        RandomAccessFile indexData = new RandomAccessFile(file, "r");
        
        double numberOfIndex = file.length() / IndexStudent.DATASIZE;

        for(int i = 0; i < numberOfIndex; i++){
            indexData.seek(i*IndexStudent.DATASIZE);

            IndexStudent indexStudent = null;
            try {
                indexStudent = IndexStudent.readData(indexData);
                indexStudents.add(indexStudent);

            } catch (EOFException e) { // para quando terminar os dados do arquivo
                indexStudent = null;
            }
            
        }
    }

    //Ordena o arquivo com os alunos
    //Passar o arquivo desordenado como parametro
    public static File sortFile(File file) throws IOException {
        RandomAccessFile studentsData = new RandomAccessFile(file, "r");

        //Serão 4 arquivos fonte
        File[] files1 = {new File("0.ord"), new File("1.ord"), new File("2.ord"), new File("3.ord")};
        File[] files2 = {new File("4.ord"), new File("5.ord"), new File("6.ord"), new File("7.ord")};

        //arquivo ordenado que será retornado
        File sortedFile = null;

        double numberOfStudents = file.length() / FixedSizeStudent.DATASIZE;
        long fileTotalLength = file.length();

        int nextBlock = BLOCK_SIZE;

        //total de passadas no arquivo
        int totalSteps = logb((int) Math.ceil(numberOfStudents/BLOCK_SIZE), FILE_SOURCES);

        for(int step = 0; step < totalSteps; step++){

            File[] initialFiles;
            File[] finalFiles;

            if(step % 2 == 0){
                initialFiles = files1;
                finalFiles = files2;
            } else {
                initialFiles = files2;
                finalFiles = files1;
            }

            for(int i = 0, countSteps = 0; i < numberOfStudents; i = nextBlock - BLOCK_SIZE , countSteps++){

                File fileOrd = initialFiles[countSteps % FILE_SOURCES];
                RandomAccessFile dataOrd = new RandomAccessFile(fileOrd, "rw");
    
                List<FixedSizeStudent> list = new ArrayList<>();
    
                for(int j = i; j < nextBlock; j++){
                    int pos = j * FixedSizeStudent.DATASIZE;
    
                    studentsData.seek(pos);
    
                    FixedSizeStudent student = null;
                    try {
                        student = FixedSizeStudent.readData(studentsData);
                        list.add(student);
                    } catch (EOFException e) {
                        student = null;
                    }
                }
    
                list = list.stream()
                           .sorted(Comparator.comparing(FixedSizeStudent::getName))
                           .collect(Collectors.toList());
    
                list.forEach((s) -> {
                    try {
                        s.saveData(dataOrd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    
                new FixedSizeStudent("", -1, -1).saveData(dataOrd);
    
                //o primeiro bloco vai até 10, o segundo ate 20 e assim por diante
                nextBlock *= 2;
            }


            //Quando todos os arquivos forem totalmente revistos, o loop acaba.
            int filesFinished = 0;

            //posicao de cada arquivo para fazer intercalacao
            //inicialmente comecando do zero
            int[] vectorPos = {0,0,0,0};

            while(filesFinished < FILE_SOURCES){

                double biggerNumberOfBlock = 0;

                for(int i = 0; i < FILE_SOURCES; i++){

                    double value = (initialFiles[i].length() / FixedSizeStudent.DATASIZE) / BLOCK_SIZE;
                    if(value > biggerNumberOfBlock){
                        biggerNumberOfBlock = value;
                    } 
                }

                for(int countSteps = 0; countSteps < numberOfStudents / (BLOCK_SIZE * FILE_SOURCES); countSteps++){
                    File fileOrd = finalFiles[countSteps % FILE_SOURCES];
                    RandomAccessFile dataOrd = new RandomAccessFile(fileOrd, "rw");

                    FixedSizeStudent[] vectorStudents = new FixedSizeStudent[BLOCK_SIZE * FILE_SOURCES];
                    FixedSizeStudent[] vAux = new FixedSizeStudent[FILE_SOURCES];

                    //loop para todos os blocos que tem no maior arquivo.
                    for(int k = 0; k < biggerNumberOfBlock; k++){

                        //loop um bloco de cada arquivo... se tem 4 arquivo e ta 10 alunos por bloco, será 40 repeticoes
                        for(int j = 0; j < BLOCK_SIZE * FILE_SOURCES; j++){
    
                            FixedSizeStudent lessStudent = vAux[0];
                            int posLessStudent = 0;
    
    
                            for(int i=0; i < FILE_SOURCES; i++){
                                RandomAccessFile data = new RandomAccessFile(initialFiles[i], "r");
        
                                data.seek(vectorPos[i]);
    
                                try {
                                    vAux[i] = FixedSizeStudent.readData(data);
    
                                    if(vAux[i].name != null && vAux[i].id != -1){
                                        lessStudent = vAux[i];
                                        posLessStudent = i;
                                    }
    
                                } catch (EOFException e) {
                                    vAux[i] = null;
                                }
        
                                
                            }
        
                            
                            for(int i=0; i < FILE_SOURCES; i++){
        
                                if( vAux[i] != null 
                                    && vAux[i].name != null
                                    && vAux[i].id != -1
                                    && vAux[i].name.compareTo(lessStudent.name) < 0){
                                        
                                    lessStudent = vAux[i];
                                    posLessStudent = i;
                                }
        
                            }
        
                            vectorPos[posLessStudent] += FixedSizeStudent.DATASIZE;
        
                            vectorStudents[j] = lessStudent;
        
                        }
    
                        for(int i=0; i < BLOCK_SIZE * FILE_SOURCES; i++){
                            vectorStudents[i].saveData(dataOrd);
                        }
                    }

                }

                //dados ordenados
                if(finalFiles[filesFinished].length() == fileTotalLength){
                    sortedFile = finalFiles[filesFinished];
                    break;
                }
                

                filesFinished ++;

            }

            if(sortedFile != null){
                break;
            }

        }

        return sortedFile;

    }

    public static int logb(int x, int base){
        return (int) Math.ceil(Math.log(x) / Math.log(base));
    }

    public static void main(String[] args) throws Exception {

        File arqDados = new File("alumni.dat");
        File arqTexto = new File("dadosTeste.txt");
        File arqIndex = new File("indexId.dat");

        loadIndex(arqIndex);

        //sortFile(arqDados);

        //textFileToBinaryFile(arqTexto, arqDados);

        //createIndexFile(arqDados, arqIndex); 


        FixedSizeStudent auxStudent;
        Scanner leitor = new Scanner(System.in);
        
        int opcao = 3;
        do{
            System.out.println("=== ARQUIVOS E ACESSO ALEATÓRIO ===");
            System.out.println("1 - Leitura arquivo texto para o binario");
            System.out.println("2 - Criar indice do arquivo");
            System.out.println("3 - Ler um registro pela posicao");
            System.out.println("4 - Ler um registro pela matricula");
            System.out.println("5 - Sair");
            opcao = leitor.nextInt();
            leitor.nextLine();
            switch(opcao){
                case 1: textFileToBinaryFile(arqTexto, arqDados);
                    break;
                case 2: createIndexFile(arqDados, arqIndex); 
                    break;
                case 3: System.out.print("Posição do registro: ");
                        int pos = leitor.nextInt();
                        auxStudent = loadData(arqDados, pos);         //chama o método de ler os dados do arquivo e os mostra no foreach abaixo
                        if(auxStudent!=null) System.out.println(auxStudent);
                        System.out.println();
                    break;
                case 4:
                    System.out.print("Matricula: ");
                    int id = leitor.nextInt();
                    auxStudent = findStudentById(id, arqDados); // chama o método de ler os dados do arquivo e os mostra no
                                                        // foreach abaixo
                    if (auxStudent != null)
                        System.out.println(auxStudent);
                    else 
                        System.out.println("Aluno nao encontrado!!");
                    System.out.println();
                break;
            }
        }while(opcao!=5);
        leitor.close(); 

    }
}