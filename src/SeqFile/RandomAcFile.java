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

public class RandomAcFile {

    static List<IndexStudent> indexStudents = new ArrayList<>();

    static final int BLOCK_SIZE = 10;
    static final int QTD_FILE_SOURCES = 4;
    static final String LAST_STUDENT_NAME = "ZZZZZZZZZZZZZZZ";

    // Procura o aluno pela sua posição no arquivo.
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
        // Esvaziando arquivo de indice
        studentIndex.setLength(0);

        // calculo do numero de alunos
        double numberOfStudents = file.length() / FixedSizeStudent.DATASIZE;

        indexStudents = new ArrayList<>();

        for (int i = 0; i < numberOfStudents; i++) {

            int pos = i * FixedSizeStudent.DATASIZE;

            studentData.seek(pos);

            FixedSizeStudent student = null;
            try {
                student = FixedSizeStudent.readData(studentData);
                indexStudents.add(new IndexStudent(student.id, pos));
            } catch (EOFException e) {
                student = null;
            }

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

    // Encontrar o aluno passando como parametro a matricula dele...
    // Esse método usa o indice para encontrar o aluno
    public static FixedSizeStudent findStudentById(int id, File file) throws IOException, ClassNotFoundException {
        RandomAccessFile studentData = new RandomAccessFile(file, "r");

        int positionStudent = findPositionStudent(id);
        if (positionStudent == -1) {
            return null;
        }

        studentData.seek(positionStudent);
        FixedSizeStudent student = null;
        try {
            student = FixedSizeStudent.readData(studentData);

        } catch (EOFException e) { // para quando terminar os dados do arquivo
            student = null;
        }

        studentData.close();
        return student;
    }

    // Encontra a posicao do aluno no arquivo principal, utilizando o indice
    public static int findPositionStudent(int id) {
        List<IndexStudent> indexes = indexStudents.stream().filter((student) -> student.id == id)
                .collect(Collectors.toList());
        if (indexes.isEmpty()) {
            return -1;
        }
        IndexStudent indexStudent = indexes.get(0);
        return indexStudent.pos;
    }

    // Carrega arquivo de indice para a memoria principal
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
    // Ordenacao externa do arquivo principal
    public static File sortFile(File file) throws IOException {

        clearContentAuxFiles();

        RandomAccessFile studentsData = new RandomAccessFile(file, "r");

        // Serão 4 arquivos fonte
        File[] files1 = { new File("0.ord"), new File("1.ord"), new File("2.ord"), new File("3.ord") };
        File[] files2 = { new File("4.ord"), new File("5.ord"), new File("6.ord"), new File("7.ord") };

        // arquivo ordenado que será retornado
        File sortedFile = null;

        double numberOfStudents = file.length() / FixedSizeStudent.DATASIZE;
        int nextStudent = 0;

        // Esse tamanho será utilizado para saber quando a ordenacao acabou
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
            FixedSizeStudent studentFlag = new FixedSizeStudent(LAST_STUDENT_NAME, -1, -1);
            try {
                studentFlag.saveData(dataOrd);
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataOrd.close();

        } // fim do loop para a gravacao dos primeiros blocos

        studentsData.close();

        // Iniciando intercalação
        boolean filesFinished = false;
        int count = 0;

        while (!filesFinished) {

            if (count % 2 == 0) {
                initialFiles = files1;
                finalFiles = files2;
            } else {
                initialFiles = files2;
                finalFiles = files1;
            }

            // LIMPANDO TODOS OS ARQUIVOS QUE VAO RECEBER OS NOVOS DADOS.
            for (int i = 0; i < QTD_FILE_SOURCES; i++) {
                RandomAccessFile clean = new RandomAccessFile(finalFiles[i], "rw");
                clean.setLength(0);
                clean.close();
            }

            // varivavel do ponteiro de posicoes para os arquivos de leitura
            int[] positionFiles2 = { 0, 0, 0, 0 };

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

                // Sempre no final do arquivo
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
                FixedSizeStudent studentFlag = new FixedSizeStudent(LAST_STUDENT_NAME, -1, -1);
                try {
                    studentFlag.saveData(ord);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Pulando o aluno flag de todos os arquivos para a proxima intercalacao
                for (int j = 0; j < QTD_FILE_SOURCES; j++) {
                    positionFiles2[j]++;
                }

                // Se o arquivo atual for maior ou igual ao arquivo original é porque todos os
                // alunos ja estao naquele arquivo, entao ele ja está ordenado com todos os
                // alunos
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

    // Apagar conteudo dos arquivos auxiliares para ordenacao
    public static void clearContentAuxFiles() {
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

    // Mostrando relatorio, paginando de 20 em 20 alunos
    public static void showContentFilePaged(File file, int startPosition) throws IOException {

        Scanner scanner = new Scanner(System.in);

        RandomAccessFile read = new RandomAccessFile(file, "r");

        String nextPage = "";

        do {

            for (int i = 0; i < 20; i++) {

                read.seek(startPosition);
                FixedSizeStudent student;
                try {
                    student = FixedSizeStudent.readData(read);

                    if (student.id != -1)
                        System.out.println(student);

                } catch (EOFException e) { // para quando terminar os dados do arquivo
                    student = null;
                }

                startPosition += FixedSizeStudent.DATASIZE;

            }

            System.out.print("Próxima página? (y/n): ");
            nextPage = scanner.nextLine();
        } while ("y".equals(nextPage));

        read.close();
    }

    public static void main(String[] args) throws Exception {

        File arqDados = new File("alumni.dat");
        File arqTexto = new File("dadosTeste.txt");
        File arqIndex = new File("indexId.dat");
        File sortedFile = null;

        boolean fileSorted = false;
        boolean indexUpdated = false;

        loadIndex(arqIndex);

        FixedSizeStudent auxStudent;
        Scanner leitor = new Scanner(System.in);

        int opcao = 3;
        do {
            System.out.println("=== ARQUIVOS E ACESSO ALEATÓRIO ===");
            System.out.println("1 - Leitura arquivo texto para o binario");
            System.out.println("2 - Criar indice do arquivo");
            System.out.println("3 - Ler um registro pela posicao");
            System.out.println("4 - Ler um registro pela matricula");
            System.out.println("5 - Exibir relatório de alunos ordenados");
            System.out.println("6 - Sair");
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
                case 5:

                    if (!fileSorted) {
                        System.out.println("Ordenando arquivo...");
                        sortedFile = sortFile(arqDados);
                        arqDados = sortedFile;
                        fileSorted = true;
                    }

                    if (sortedFile == null) {
                        System.out.println("Ocorreu um erro ao ordenar arquivo!");
                        break;
                    }

                    System.out.print("Deseja começar de uma matricula específica? (y/n): ");
                    String resp = leitor.nextLine();
                    if ("y".equals(resp)) {
                        System.out.print("Digite a matricula de inicio: ");
                        int idStudent = leitor.nextInt();

                        if (!indexUpdated) {
                            System.out.println("Atualizando indices...");
                            createIndexFile(sortedFile, arqIndex);
                            loadIndex(arqIndex);
                            indexUpdated = true;
                        }

                        int position = findPositionStudent(idStudent);

                        showContentFilePaged(sortedFile, position);

                    } else {
                        showContentFilePaged(sortedFile, 0);
                    }
                    break;
            }
        } while (opcao != 6);
        leitor.close();

    }
}