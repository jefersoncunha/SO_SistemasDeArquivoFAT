
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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    do{
      System.out.println("#------------- TEST CASE: ------------- #");
      System.out.println("| 1. Create ");
      System.out.println("| 2. Append");
      System.out.println("| 3. Read");
      System.out.println("| 4. Remove");
      System.out.println("| 5. Show Directory");
      System.out.println("| 6. Show Free Space");
      System.out.println("| 0. Quit");
      System.out.println("\n Make Your Choice: ");
      option = scan.nextInt();

      if(option ==1) createFile();
      else if(option ==2) appendFile();
      else if(option ==3) readFile();
      else if(option ==4) removeFile();
      else if(option ==5) showDirectory();
      else if(option ==6) showfreeSpace();
    }while(option != 0);

  }

  @Override
  public void create(String fileName, byte[] data) {
    ByteBuffer dice = ByteBuffer.wrap(data);
    int fileSize = dice.capacity();  // tamanho total do arquivo
    int blockAmount = (fileSize/TAM_BLOCOS) + 1;  // quantidade de blocos que ele ocupa
    byte[] dataItem = new byte[TAM_BLOCOS];
    int[] freeBlocks = new int[blockAmount];  // lista dos blocos que este arquivo vai ocupar

    try {
      int freeBlock = disco.freeBlock();  // encontra um bloco livre
      if(freeBlock <= 1 || freeBlock > NUM_BLOCOS)
        throw new IllegalStateException("Bloco invalido");

      EntradaDiretorio dir = new EntradaDiretorio();
        dir.setNomeArquivo(fileName);
        dir.setTamanho(fileSize);
        dir.setPrimeiroBloco(freeBlock);
        diretorioRaiz.add(dir);

      if(fileSize < TAM_BLOCOS){
        FAT[freeBlock] = 0;
        disco.escreveBloco(freeBlock, data);
      } else {
          for(int i=0; i<blockAmount; i++){
              freeBlocks[i] = freeBlock;
              freeBlock = disco.freeBlock();

              if(freeBlock <= 1 || freeBlock > NUM_BLOCOS)
                throw new IllegalStateException("Bloco invalido");

              FAT[freeBlocks[i]] = freeBlock;

              if(i == blockAmount-1)
                  System.arraycopy(dataItem, i * TAM_BLOCOS, dataItem, 0, (fileSize - (i * TAM_BLOCOS)));
               else
                  System.arraycopy(dataItem, i * TAM_BLOCOS, dataItem, 0, TAM_BLOCOS);

              disco.escreveBloco(freeBlocks[i], dataItem);
          }
          FAT[freeBlocks[blockAmount-1]] = 0;
      }
      escreveDiretorio();
      escreveFAT();

    } catch (IOException ex) { }
  }

  @Override
  public void append(String fileName, byte[] data) {
    int found = findNumber(fileName);
    try {
        int fileSize     = diretorioRaiz.get(found).tamanho;
        int firstBlock   = diretorioRaiz.get(found).primeiroBloco;
        if(fileSize < TAM_BLOCOS) {
            byte[] dados = disco.leBloco(firstBlock);
            if((fileSize + data.length) <= TAM_BLOCOS) {
                System.arraycopy(data, 0, dados, fileSize+1, data.length);
                disco.escreveBloco(firstBlock, dados);
                diretorioRaiz.get(found).tamanho += data.length;
                escreveDiretorio();
            } else
                System.out.println("@ append error: TAM_BLOCOS excedido.");

        }else
            System.out.println("@ append error: fileSize > TAM_BLOCOS");

    } catch (Exception ex) {}
  }

  @Override
  public byte[] read(String fileName, int offset, int limit) {
    int primeiroBloco = -1;
    for(int i=0; i<diretorioRaiz.size() && primeiroBloco == -1; i++){
        if(diretorioRaiz.get(i).nomeArquivo.equals(fileName)){
            primeiroBloco = diretorioRaiz.get(i).primeiroBloco;
        }
    }
    try {
        ByteBuffer dados = ByteBuffer.wrap(disco.leBloco(primeiroBloco));
        byte[] texto = new byte[limit-offset];
        System.arraycopy(dados.array(), offset, texto, 0, (limit-offset));
        return texto;

    } catch (IOException ex) { }

    return null;
  }

/*
| Encontrar o Elemento que deseja remover buscando o nome
| Remover o Elemento do ArrayList<EntradaDiretorio> diretorioRaiz
| Remove da FAT o indice do arquivo desejado
*/
  @Override
  public void remove(String fileName) {
      int found = findNumber(fileName);
      diretorioRaiz.remove(found);
      FAT[diretorioRaiz.get(found).primeiroBloco]= -1;
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

  private void readFile(){
    Scanner scan = new Scanner(System.in);
    int offset; int limit; int foundName;

    System.out.println("#------------ Read File: ------------ #");

    do{
      foundName = findName();
    }while(foundName < 0 );

    System.out.println("| File Name:"+diretorioRaiz.get(foundName).nomeArquivo);
    System.out.println("| File Size: " +diretorioRaiz.get(foundName).tamanho);

    System.out.println("| Start Read my file in position: ");
    do {
      offset = scan.nextInt();
    }while(offset < 0 || offset > diretorioRaiz.get(foundName).tamanho - 1);

    System.out.println("| Stop Read (until the end -1 ): ");
    do {
      limit = scan.nextInt();
    } while(limit < -1 || limit == 0 || limit > diretorioRaiz.get(foundName).tamanho);

    if(limit == -1)
        limit = diretorioRaiz.get(foundName).tamanho;

    byte[] data = read(diretorioRaiz.get(foundName).nomeArquivo, offset, limit);
    System.out.println("| File Content: " +new String(data)+"\n");

  }

  public void createFile(){
    Scanner scan = new Scanner(System.in);
    String fileName ="", fileContent ="";
    System.out.println("#------------ Create File: ------------ #");

    do{
      System.out.println("| Ex: file.txt \n| FileName: ");
      fileName = scan.nextLine();
      fileName = processFileName(fileName);
    }while(fileName.equals("fail"));

    System.out.println("| File Content : ");
    fileContent = scan.nextLine();
    byte[] data = fileContent.getBytes();

    create(fileName, data);
  }

  public void appendFile(){
    Scanner scan = new Scanner(System.in);
    String  fileContent ="";
    int     foundName;

    System.out.println("#------------ Append File: ------------ #");
    do{
      foundName = findName();
    }while(foundName < 0 );

    System.out.println("| Content: ");
    fileContent = scan.nextLine();

    byte[] data = fileContent.getBytes();
    append(diretorioRaiz.get(foundName).nomeArquivo, data);
  }

  public void removeFile(){
    int foundName;
    System.out.println("#------------ Remove File: ------------ #");
    do{
      foundName = findName();
    }while(foundName < 0 );
    remove(diretorioRaiz.get(foundName).nomeArquivo);
  }

  public int findName(){
    Scanner scan = new Scanner(System.in);
    System.out.println("| Insert Name to find: ");
    String fileName = scan.nextLine();
    fileName = processFileName(fileName);

    for(int i=0; i<diretorioRaiz.size(); i++){
      if(diretorioRaiz.get(i).nomeArquivo.equals(fileName))
        return i;
    }
    System.out.println("@ Sorry this name can't be found :( ");
    return -1;
  }

  public int findNumber(String fileName){
    for(int i=0; i<diretorioRaiz.size(); i++){
      if(diretorioRaiz.get(i).nomeArquivo.equals(fileName))
        return i;
    }
    return -1;
  }

/*
| For(char) verifica se o nome contem extensao
| condicao check true
|  Recebe o nome compleco com a extensao e divide o mesmo
|  Subustitui todos os espacos por _
|  Se os tamanhos para a extensao ou arquivo forem inferiores aos exigido
|   levantamos um ERROR;
| condicao check false
|   lenvantamos erro na extensao
*/
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
            System.out.println("@ processFileName ERROR:");
        }
        String newName = splitedFileName[0] + "." + splitedFileName[1];
        return newName;
    }
    else{
        System.out.println("@ processFileName error ext into"+fileName+" :(");
        return "fail";
    }
  }

/*
| Processar novamente o nome e remove os _
| deixando a escrita mais limpa para o usuario
*/
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
/*
| Mostrar Informacoes sobre o diretorio
*/
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
    System.out.println("\n");
  }

  /*
  | Mostrar Informacoes sobre o espaco no arquivo
  */
  public void showfreeSpace(){
    int value;
    value = freeSpace();
    System.out.println("#------------ showfreeSpace ------------ #");
    System.out.println("@ freeSpace :" +value/1024+ " KBytes"+"\n");
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
