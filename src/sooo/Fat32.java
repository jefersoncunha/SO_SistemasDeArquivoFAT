
package sooo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

public class Fat32 implements SistemaArquivos{

    private final int NUM_BLOCOS = 200;
    private final int TAM_BLOCOS = 65536;
    private final int[] FAT = new int[NUM_BLOCOS];
    private final int QTD_BLOCOS_FAT = ((NUM_BLOCOS * 4) / TAM_BLOCOS) + 1;
    private DriverDisco disco;
    private Collection<EntradaDiretorio> diretorioRaiz = new ArrayList<EntradaDiretorio>();
    
    public static void main(String[] args) throws IOException {
        Fat32 fat = new Fat32();
    }
    
    private void casoTeste() {
        
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
    
    @Override
    public void create(String fileName, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void append(String fileName, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            for(int j=0; j < 8; j++){
                char c = bbuffer.getChar();
                if(c != ' ')
                    sb.append(c);
            }
            sb.append('.');
            for(int j=0; j < 3; j++){
                char c = bbuffer.getChar();
                if(c != ' ')
                    sb.append(c);
            }
            entr.nomeArquivo = sb.toString();
            entr.tamanho = bbuffer.getInt();
            entr.primeiroBloco = bbuffer.getInt();
            diretorioRaiz.add(entr);
        }
    }

    private void criaDiretorio() throws IOException {
        ByteBuffer bbuffer = ByteBuffer.allocate(TAM_BLOCOS);
        bbuffer.putInt(0);
        disco.escreveBloco(0, bbuffer.array());
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
    
    private class EntradaDiretorio {
        private String nomeArquivo;
        private String extencaoArquivo;
        private int primeiroBloco;
        private int tamanho;
        
    }
    
}
