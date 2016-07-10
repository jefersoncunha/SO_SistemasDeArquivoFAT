
package sooo;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DriverDisco {

    private final int TAM_BLOCO;
    private final int NUM_BLOCO;
    private RandomAccessFile rf;
    private boolean formatado;

    public boolean isFormatado() {
        return formatado;
    }

    public DriverDisco(int tamBloco, int numBlocos) throws IOException {
      this.NUM_BLOCO = numBlocos;
      this.TAM_BLOCO = tamBloco;
      File f = new File("disco.fat");

      if(f.exists()){
          rf = new RandomAccessFile(f, "rw");
          if(rf.length() != numBlocos * tamBloco){
              throw new IllegalStateException("Disco com tamanho invalido");
          }
          formatado = true;
      }else{
          f.createNewFile();
          rf = new RandomAccessFile(f, "rw");
          rf.setLength(numBlocos * tamBloco);
          //rf.seek((tamBloco * numBlocos) - 1);
          //rf.write(new byte[] { 0 });
          rf.getChannel().force(true);
          formatado = false;
      }
    }

    public void escreveBloco(int nBloco, byte[] dados) throws IOException{
      if(nBloco >= NUM_BLOCO) {
          throw new IllegalStateException("Numero de bloco invalido");
      }
      if(dados.length > TAM_BLOCO){
          throw new IllegalStateException("Bloco com tamanho invalido");
      }
      rf.seek(nBloco * TAM_BLOCO);
      rf.write(dados);
      rf.getChannel().force(true);
      System.out.println("[DISCO]: " +nBloco);
    }

    public byte[] leBloco(int nBloco) throws IOException{
      if(nBloco >= NUM_BLOCO) {
          throw new IllegalStateException("Numero de bloco invalido");
      }
      byte[] dados = new byte [TAM_BLOCO];
      rf.seek(nBloco * TAM_BLOCO);
      rf.read(dados);

      return dados;
    }

/*
| freeBlock() procura blocos livres no disco, da possicao 0 a 200;
| faco() faz uma pesquisa aleatoria para encontrar blocos livres
| condicao(tryRandom) vamos busca sequencial caso n tenha encontrado na busca aleatoria
*/
    public int freeBlock() throws IOException {
      int blockNumber;
      int tryRandom = 0;
      Random rand = new Random();
      ByteBuffer bloco = ByteBuffer.allocate(TAM_BLOCO);

      do{
          blockNumber = rand.nextInt(194)+5;
          block = ByteBuffer.wrap(leBloco(blockNumber));
          tryRandom++;
      }while(bloco.getInt() != 0 && tryRandom < NUM_BLOCO);

      if(tryRandom >= NUM_BLOCO){
          blockNumber = -1;
          for(int i=2; i<NUM_BLOCO && tryRandom >= NUM_BLOCO; i++){
              block = ByteBuffer.wrap(leBloco(i));
              if(block.getInt() == 0){
                  blockNumber = i;
                  tryRandom = -1;
              }
          }
      }
      return blockNumber;
    }

}
