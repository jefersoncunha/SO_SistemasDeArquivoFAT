
package sooo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import static java.lang.System.out;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public class Fat32 implements SistemaArquivos{

    private final int NUM_BLOCOS = 200;
    private final int TAM_BLOCOS = 65536;
    private final int[] FAT = new int[NUM_BLOCOS];
    private final int QTD_BLOCOS_FAT = ((NUM_BLOCOS * 4) / TAM_BLOCOS) + 1;
    private DriverDisco disco;
    private ArrayList<EntradaDiretorio> diretorioRaiz = new ArrayList<EntradaDiretorio>();

    public static void main(String[] args) throws IOException {
        Fat32 fat = new Fat32();
        fat.casoTeste();
    }

     public Fat32() throws IOException {
        System.out.println("#--- Fat32() ");
        disco = new DriverDisco(TAM_BLOCOS, NUM_BLOCOS);
        if(!disco.isFormatado()){
            formataDisco();
        }else {
            leDiretorio();
            leFAT();
        }
    }


    private void casoTeste() {
      int option;
      Scanner scan = new Scanner(System.in);

      System.out.println("#------------ TEST CASE: ------------ #");

      do{
        System.out.println("| 1. Create ");
        System.out.println("| 2. Append");
        System.out.println("| 3. Read");
        System.out.println("| 4. Remove");
        System.out.println("| 5. Show Directory");
        System.out.println("| 0. Quit");
        System.out.println("\n Make Your Choice: ");
        option = scan.nextInt();

        switch(option){
          case 1:{ createFile(); }
          case 2:{ appendFile(); }
          case 3:{ }
          case 4:{ }
          case 5:{ showDirectory(); }
        };

      }while(option != 0);

    }




    @Override
    public void create(String fileName, byte[] data) {
      System.out.println("###--- create(fileName,data) ---");
      ByteBuffer dice = ByteBuffer.wrap(data);
      int fileSize = dice.capacity();  // tamanho total do arquivo
      int blockAmount = (fileSize/TAM_BLOCOS) + 1;  // quantidade de blocos que ele ocupa
      int[] freeBlocks = new int[blockAmount];  // lista dos blocos que este arquivo vai ocupar
      byte[] dataItem = new byte[TAM_BLOCOS];

      try {
          int freeBlock = disco.freeBlock();  // encontra um bloco livre

          if(freeBlock <= 1 || freeBlock > NUM_BLOCOS)
            throw new IllegalStateException("Bloco invalido");

          EntradaDiretorio dir = new EntradaDiretorio();
            dir.setNomeArquivo(fileName);
            dir.setTamanho(fileSize);
            dir.setPrimeiroBloco(freeBlock);
            diretorioRaiz.add(dir);  // cria espaço no diretorio

          if(fileSize > TAM_BLOCOS){  // se o arquivo ocupa mais de 1 bloco

              for(int i=0; i<blockAmount; i++){
                  freeBlocks[i] = freeBlock;
                  freeBlock = disco.freeBlock();

                  if(freeBlock <= 1 || freeBlock > NUM_BLOCOS)
                    throw new IllegalStateException("Bloco invalido");

                  FAT[freeBlocks[i]] = freeBlock;

                  if(i == blockAmount-1)  // se for a ultima parte, le só a parte que falta
                      System.arraycopy(dataItem, i * TAM_BLOCOS, dataItem, 0, (fileSize - (i * TAM_BLOCOS)));
                   else
                      System.arraycopy(dataItem, i * TAM_BLOCOS, dataItem, 0, TAM_BLOCOS);

                  disco.escreveBloco(freeBlocks[i], dataItem);
              }
              FAT[freeBlocks[blockAmount-1]] = 0;
          } else {
              FAT[freeBlock] = 0;
              disco.escreveBloco(freeBlock, data);
          }
          escreveDiretorio();
          escreveFAT();

      } catch (IOException ex) { }
    }

    @Override
    public void append(String fileName, byte[] data) {
        boolean encontrado = false;

        for(int i=0; i<diretorioRaiz.size() && encontrado == false; i++){
            if(diretorioRaiz.get(i).nomeArquivo.equals(fileName)){
                int tamanho = diretorioRaiz.get(i).tamanho;
                int primeiroBloco = diretorioRaiz.get(i).primeiroBloco;

                try {
                    if(tamanho < TAM_BLOCOS) {  // ocupa so um bloco
                        byte[] dados = disco.leBloco(primeiroBloco);
                        if((tamanho + data.length) <= TAM_BLOCOS) {  // cabe tudo em um bloco
                            System.arraycopy(data, 0, dados, tamanho+1, data.length);
                            disco.escreveBloco(primeiroBloco, dados);
                            diretorioRaiz.get(i).tamanho += data.length;
                            escreveDiretorio();
                        } else {  // aumenta o numero de blocos

                        }
                    } else {

                    }
                } catch (Exception ex) {}

                encontrado = true;
            }
        }
    }

    @Override
    public byte[] read(String fileName, int offset, int limit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void remove(String fileName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int freeSpace() {
        int espaco = 0;
        for(int i=0; i < FAT.length; i++){
            if(FAT[i] == -1){
                espaco += TAM_BLOCOS;
            }
        }
        return espaco;
    }

    private void formataDisco() throws IOException {
        criaDiretorio();
        criaFat();
    }

    private void leDiretorio() throws IOException {
        byte[] bloco = disco.leBloco(0);
        ByteBuffer bbuffer = ByteBuffer.wrap(bloco);
        int quant = bbuffer.getInt();
        for(int i=0; i < quant; i++){
            EntradaDiretorio entr = new EntradaDiretorio();
            StringBuffer sb = new StringBuffer();
             for(int j=0; j < 12; j++){
                 char c = bbuffer.getChar();
                    sb.append(c);
             }
            entr.setNomeArquivo(sb.toString());
            entr.setTamanho(bbuffer.getInt());
            entr.setPrimeiroBloco(bbuffer.getInt());
            diretorioRaiz.add(entr);
        }
    }


    private void criaDiretorio() throws IOException {
        ByteBuffer bbuffer = ByteBuffer.allocate(TAM_BLOCOS);
        bbuffer.putInt(0);
        disco.escreveBloco(0, bbuffer.array());
    }

    private void escreveDiretorio() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(TAM_BLOCOS);
        int bloco = 0;
        b.putInt(diretorioRaiz.size());
        for(int i=0; i < diretorioRaiz.size(); i++){
            for(int j=0; j<12; j++){
                char c = diretorioRaiz.get(i).nomeArquivo.charAt(j);
                b.putChar(c);
            }
            b.putInt(diretorioRaiz.get(i).tamanho);
            b.putInt(diretorioRaiz.get(i).primeiroBloco);
            if(b.position() == TAM_BLOCOS){
                disco.escreveBloco(bloco, b.array());
                bloco++;
            }
        }
        disco.escreveBloco(bloco, b.array());
    }


    private void criaFat() throws IOException {
        FAT[0] = 0;
        for(int i = 1; i <= QTD_BLOCOS_FAT; i++){
            FAT[i] = 0;
        }
        for(int i = QTD_BLOCOS_FAT + 1; i < FAT.length; i++){
            FAT[i] = -1;
        }
        escreveFAT();
    }

    private void leFAT() throws IOException {
        byte[] bbuffer = new byte[QTD_BLOCOS_FAT * TAM_BLOCOS];
        for(int i=0; i < QTD_BLOCOS_FAT; i++){
            byte[] bloco = disco.leBloco(i+1);
            ByteBuffer buf = ByteBuffer.wrap(bloco);
            System.arraycopy(bloco, 0, bbuffer, i * TAM_BLOCOS, TAM_BLOCOS);
        }
        ByteBuffer buf = ByteBuffer.wrap(bbuffer);
        for(int i=0; i < FAT.length; i++){
            FAT[i] = buf.getInt();
        }
    }

    private void escreveFAT() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(TAM_BLOCOS);
        int bloco = 1;
        for(int i=0; i < FAT.length; i++){
            b.putInt(FAT[i]);
            if(b.position() == TAM_BLOCOS){
                disco.escreveBloco(bloco, b.array());
                bloco++;
            }
        }
        disco.escreveBloco(bloco, b.array());
    }

    public void createFile(){
      Scanner scan = new Scanner(System.in);
      String fileName ="", fileContent ="";
      System.out.println("| File Name: \n| Ex: arquivo.tx \n| Digite o nome do Arquivo: ");

      fileName = scan.nextLine();
      fileName = processFileName(fileName);

      System.out.println("| File Content : ");
      fileContent = scan.nextLine();
      byte[] data = fileContent.getBytes();

      create(fileName, data);
    }

    public void appendFile(){
      Scanner scan = new Scanner(System.in);
      String fileName ="", fileContent ="";
      System.out.println("#------------ Append File: ------------ #");
      System.out.println("| File Name: ");

      scan.nextLine();
      fileName = scan.nextLine();
      System.out.println("Conteudo: ");
      fileContent = scan.nextLine();

      byte[] data = fileContent.getBytes();

      append(fileName, data);
    }


    public String processFileName(String fileName) {
      char analyse[] = fileName.toCharArray();
      boolean check = false;
      for (char a : analyse) {
          if (a == '.') {
              check = true;
          }
      }
      if (check == true) {
          String splitedFileName[] = fileName.split("\\.");
          try {
              while (splitedFileName[0].length() != 8) {
                  if (splitedFileName[0].length() < 8)
                      splitedFileName[0] += "_";
                  else if (splitedFileName[0].length() > 8)
                      splitedFileName[0] = splitedFileName[0].substring(0, 7);
              }
              while (splitedFileName[1].length() != 3) {
                  if (splitedFileName[1].length() < 3)
                      splitedFileName[1] += "_";
                  else if (splitedFileName[1].length() > 3)
                      splitedFileName[1] = splitedFileName[1].substring(0, 3);
              }
          } catch (ArrayIndexOutOfBoundsException q) {
              System.out.println("# processFileName() ERROR:");
          }
          String newName = splitedFileName[0] + "." + splitedFileName[1];
          return newName;
      }
      else{
          return "fail";
      }
    }

    public void showDirectory(){

      System.out.println("#------------ Show Directory: ------------ #");
      for(int i=0; i<diretorioRaiz.size(); i++){
          System.out.println("|------------ FILE: "+i+" ------------ \n|");
          System.out.println("| Name : " +printProcessedName(diretorioRaiz.get(i).nomeArquivo));
          System.out.println("| Size : " +diretorioRaiz.get(i).tamanho);
          System.out.println("| Block: " +diretorioRaiz.get(i).primeiroBloco+"\n");
      }
      System.out.println("\n#------------ FAT Blocks: ------------ #");
      for(int i=0; i<NUM_BLOCOS; i++){
          if(FAT[i] != -1 )
              System.out.println("| FAT[" +i +"]: " +FAT[i]);
      }

    }
    
    public String printProcessedName(String fileName){
        char analyse[] = fileName.toCharArray();
        String name = "";
        for (char a : analyse){
            if (a != '_'){
                name = name + a;
            }  
        }
        return name;
    }

    private class EntradaDiretorio {
        private String nomeArquivo;
        private String extencaoArquivo;
        private int primeiroBloco;
        private int tamanho;

        public String getNomeArquivo() {
            return nomeArquivo;
        }

        public void setNomeArquivo(String nomeArquivo) {
            this.nomeArquivo = nomeArquivo;
        }

        public int getPrimeiroBloco() {
            return primeiroBloco;
        }

        public void setPrimeiroBloco(int primeiroBloco) {
            this.primeiroBloco = primeiroBloco;
        }

        public int getTamanho() {
            return tamanho;
        }

        public void setTamanho(int tamanho) {
            this.tamanho = tamanho;
        }

    }

}
