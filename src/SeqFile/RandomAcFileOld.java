package SeqFile;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RandomAcFileOld {

    static List<IndexStudent> indexStudents = new ArrayList<>();

    static final int BLOCK_SIZE = 10;
    static final int QTD_FILE_SOURCES = 4;
    static final String LAST_STUDENT_NAME = "ZZZZZZZZZZZZZZZ";

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
        indexStudents.sort((a, b) -> a.compareTo(b));

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

        List<IndexStudent> indexes = indexStudents.stream().filter((student) -> student.id == id)
                .collect(Collectors.toList());
        if (indexes.isEmpty()) {
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

    // Carregando indice para a memoria principal
    public static void loadIndex(File file) throws IOException {
        RandomAccessFile indexData = new RandomAccessFile(file, "r");

        double numberOfIndex = file.length() / IndexStudent.DATASIZE;

        for (int i = 0; i < numberOfIndex; i++) {
            indexData.seek(i * IndexStudent.DATASIZE);

            IndexStudent indexStudent = null;
            try {
                indexStudent = IndexStudent.readData(indexData);
                indexStudents.add(indexStudent);

            } catch (EOFException e) { // para quando terminar os dados do arquivo
                indexStudent = null;
            }

        }
    }

    // Intercalação balanceada de vários caminhos
    public static File sortFile2(File file) throws IOException {
        RandomAccessFile studentsData = new RandomAccessFile(file, "r");

        // Serão 4 arquivos fonte
        File[] files1 = { new File("0.ord"), new File("1.ord"), new File("2.ord"), new File("3.ord") };
        File[] files2 = { new File("4.ord"), new File("5.ord"), new File("6.ord"), new File("7.ord") };

        // arquivo ordenado que será retornado
        File sortedFile = null;

        double numberOfStudents = file.length() / FixedSizeStudent.DATASIZE;
        int nextStudent = 0;

        double originalFileLength = file.length();

        // arquivos para leitura em blocos
        File[] initialFiles = files1;

        // arquivos para intercalacao dos blocos gerados
        File[] finalFiles = files2;

        // Geração dos primeiros blocos
        for (int i = 0, step = 0; i < numberOfStudents; i = nextStudent, step++) {

            nextStudent = i + BLOCK_SIZE;

            File fileOrd = initialFiles[step % QTD_FILE_SOURCES];

            // arquivo com bloco ordenado
            RandomAccessFile dataOrd = new RandomAccessFile(fileOrd, "rw");

            // sempre comecando a preencher do final do arquivo
            dataOrd.seek(fileOrd.length());

            List<FixedSizeStudent> list = new ArrayList<>();

            for (int j = i; j < nextStudent; j++) {
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

            // ordanacao da lista pelo nome
            list = list.stream().sorted(Comparator.comparing(FixedSizeStudent::getName)).collect(Collectors.toList());

            list.forEach((s) -> {
                try {
                    s.saveData(dataOrd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // salvando usuario nulo como flag para fim do bloco
            FixedSizeStudent studentFlag = new FixedSizeStudent("", -1, -1);
            try {
                studentFlag.saveData(dataOrd);
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataOrd.close();

        } // fim do loop para a gravacao dos primeiros blocos

        studentsData.close();

        // Geração dos primeiros blocos

        int[] positionFiles = { 0, 0, 0, 0 };

        double biggerFileLength = initialFiles[0].length();

        for (int i = 1; i < QTD_FILE_SOURCES; i++) {
            if (initialFiles[i].length() > biggerFileLength) {
                biggerFileLength = initialFiles[i].length();
            }
        }

        double biggerNumberOfBlocks = (biggerFileLength / FixedSizeStudent.DATASIZE) / BLOCK_SIZE;

        for (int i = 0; i < biggerNumberOfBlocks; i++) {

            // arquivo com intercalacao de blocos ordenados
            RandomAccessFile ord = new RandomAccessFile(finalFiles[i % QTD_FILE_SOURCES], "rw");

            ord.seek(finalFiles[i % QTD_FILE_SOURCES].length());

            int numberOfEndBlock = 0;

            // Fazendo a primeira intercalacao dos arquivos...
            while (numberOfEndBlock < QTD_FILE_SOURCES) {

                FixedSizeStudent actualStudent;
                FixedSizeStudent firstStudent = new FixedSizeStudent(LAST_STUDENT_NAME, -1, -1);
                int positionFirstStudent = -1;

                for (int j = 0; j < QTD_FILE_SOURCES; j++) {
                    RandomAccessFile read = new RandomAccessFile(initialFiles[j], "r");
                    read.seek(positionFiles[j] * FixedSizeStudent.DATASIZE);

                    try {
                        actualStudent = FixedSizeStudent.readData(read);
                    } catch (EOFException e) { // para quando terminar os dados do arquivo
                        actualStudent = null;
                    }

                    if (actualStudent != null && actualStudent.id == -1) {
                        numberOfEndBlock++;
                    }

                    if (actualStudent == null || actualStudent.id == -1) {
                        continue;
                    }

                    if (actualStudent.name.compareTo(firstStudent.name) < 0) {
                        firstStudent = actualStudent;
                        positionFirstStudent = j;
                    }

                    read.close();
                }

                if (firstStudent.id != -1 && positionFirstStudent != -1) {
                    positionFiles[positionFirstStudent]++;
                    firstStudent.saveData(ord);
                }

            }

            // salvando usuario nulo como flag para fim do bloco
            FixedSizeStudent studentFlag = new FixedSizeStudent("", -1, -1);
            try {
                studentFlag.saveData(ord);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int j = 0; j < QTD_FILE_SOURCES; j++) {
                positionFiles[j]++;
            }

            ord.close();

        }

        boolean filesFinished = false;
        int count = 0;

        while (!filesFinished) {

            if (count % 2 == 0) {
                initialFiles = files2;
                finalFiles = files1;
            } else {
                initialFiles = files1;
                finalFiles = files2;
            }

            // LIMPANDO TODOS OS ARQUIVOS QUE VAO RECEBER OS NOVOS DADOS.
            for (int i = 0; i < QTD_FILE_SOURCES; i++) {
                RandomAccessFile clean = new RandomAccessFile(finalFiles[i], "rw");
                clean.setLength(0);
                clean.close();
            }

            // Continuacao da intercalacao
            int[] positionFiles2 = { 0, 0, 0, 0 };

            biggerFileLength = initialFiles[0].length();

            for (int i = 1; i < QTD_FILE_SOURCES; i++) {
                if (initialFiles[i].length() > biggerFileLength) {
                    biggerFileLength = initialFiles[i].length();
                }
            }

            biggerNumberOfBlocks = (biggerFileLength / FixedSizeStudent.DATASIZE) / BLOCK_SIZE;

            for (int i = 0; i < biggerNumberOfBlocks; i++) {

                // arquivo com intercalacao de blocos ordenados
                RandomAccessFile ord = new RandomAccessFile(finalFiles[i % QTD_FILE_SOURCES], "rw");

                ord.seek(finalFiles[i % QTD_FILE_SOURCES].length());

                int numberOfEndBlock = 0;

                List<FixedSizeStudent> list = new ArrayList<>();

                // Fazendo a primeira intercalacao dos arquivos...
                while (numberOfEndBlock < QTD_FILE_SOURCES) {

                    for (int j = 0; j < QTD_FILE_SOURCES; j++) {
                        RandomAccessFile read = new RandomAccessFile(initialFiles[j], "r");
                        read.seek(positionFiles2[j] * FixedSizeStudent.DATASIZE);
                        FixedSizeStudent actualStudent = new FixedSizeStudent(LAST_STUDENT_NAME, 0, 0);

                        while (actualStudent != null || (actualStudent != null && actualStudent.id != -1)) {
                            try {
                                actualStudent = FixedSizeStudent.readData(read);
                            } catch (EOFException e) { // para quando terminar os dados do arquivo
                                actualStudent = null;
                            }

                            if (actualStudent != null && actualStudent.id == -1) {
                                numberOfEndBlock++;
                            }

                            if (actualStudent == null || actualStudent.id == -1) {
                                continue;
                            }

                            list.add(actualStudent);
                        }

                        read.close();
                    }

                }

                list = list.stream().sorted(Comparator.comparing(FixedSizeStudent::getName))
                        .collect(Collectors.toList());

                list.forEach((s) -> {
                    try {
                        s.saveData(ord);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // salvando usuario nulo como flag para fim do bloco
                FixedSizeStudent studentFlag = new FixedSizeStudent("", -1, -1);
                try {
                    studentFlag.saveData(ord);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < QTD_FILE_SOURCES; j++) {
                    positionFiles2[j]++;
                }

                if (finalFiles[i % QTD_FILE_SOURCES].length() >= originalFileLength) {
                    sortedFile = finalFiles[i % QTD_FILE_SOURCES];
                    filesFinished = true;
                    ord.close();
                    break;
                }

                ord.close();

            }

            count++;

        }

        return sortedFile;
    }

    public static void main(String[] args) throws Exception {

        File arqDados = new File("alumni.dat");
        File arqTexto = new File("dadosTeste.txt");
        File arqIndex = new File("indexId.dat");

        loadIndex(arqIndex);

        File sortedFile = sortFile2(arqDados);

        double quantStudents = sortedFile.length() / FixedSizeStudent.DATASIZE;

        System.out.println("-------------------- COMECANDO A MOSTRAR ARQUIVO --------------------");

        RandomAccessFile read = new RandomAccessFile(sortedFile, "r");

        for (int i = 0; i < quantStudents; i++) {
            read.seek(i * FixedSizeStudent.DATASIZE);
            FixedSizeStudent student;
            try {
                student = FixedSizeStudent.readData(read);

                if (student.id != -1)
                    System.out.println(student);
                else
                    System.out.println("---------------- ALUNO FLAG ----------------");

            } catch (EOFException e) { // para quando terminar os dados do arquivo
                student = null;
            }
        }

        read.close();

        System.out.println("---------------- TERMINANDO ARQUIVO ----------------");

        // textFileToBinaryFile(arqTexto, arqDados);

        // createIndexFile(arqDados, arqIndex);

        FixedSizeStudent auxStudent;
        Scanner leitor = new Scanner(System.in);

        int opcao = 3;
        do {
            System.out.println("=== ARQUIVOS E ACESSO ALEATÓRIO ===");
            System.out.println("1 - Leitura arquivo texto para o binario");
            System.out.println("2 - Criar indice do arquivo");
            System.out.println("3 - Ler um registro pela posicao");
            System.out.println("4 - Ler um registro pela matricula");
            System.out.println("5 - Sair");
            opcao = leitor.nextInt();
            leitor.nextLine();
            switch (opcao) {
                case 1:
                    textFileToBinaryFile(arqTexto, arqDados);
                    break;
                case 2:
                    createIndexFile(arqDados, arqIndex);
                    break;
                case 3:
                    System.out.print("Posição do registro: ");
                    int pos = leitor.nextInt();
                    auxStudent = loadData(arqDados, pos); // chama o método de ler os dados do arquivo e os mostra no
                                                          // foreach abaixo
                    if (auxStudent != null)
                        System.out.println(auxStudent);
                    System.out.println();
                    break;
                case 4:
                    System.out.print("Matricula: ");
                    int id = leitor.nextInt();
                    auxStudent = findStudentById(id, arqDados); // chama o método de ler os dados do arquivo e os mostra
                                                                // no
                    // foreach abaixo
                    if (auxStudent != null)
                        System.out.println(auxStudent);
                    else
                        System.out.println("Aluno nao encontrado!!");
                    System.out.println();
                    break;
            }
        } while (opcao != 5);
        leitor.close();

    }
}