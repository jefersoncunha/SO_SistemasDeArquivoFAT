Desenvolver uma classe em Java que implemente um **Sistema de Arquivos FAT 32** através da
interface a seguir:



```
#!java

public interface SistemaArquivos {

    /**
     * Cria um novo arquivo.
     * @param fileName nome do arquivo para criar
     * @param data dados a serem salvos
     */
     public void create(String fileName, byte[] data);
     /**
     * Adiciona dados ao final do arquivo.
     * @param fileName nome do arquivo
     * @param data dados a serem adicionados
     */
     public void append(String fileName, byte[] data);
     /**
     * Lê arquivo.
     * @param fileName nome do arquivo
     * @param offset a partir de qual posição a leitura deve ser feita.
     * @param limit até aonde a leitura será feita, -1 para ler até o final do arquivo.
     * @return dados lidos
     */
     public byte[] read(String fileName, int offset, int limit);
     /**
     * Remove o arquivo.
     * @param fileName
     */
     public void remove(String fileName);
     /**
     * Calcula o espaço disponível no sistema de arquivos.
     * @return bytes disponíveis
     */
     public int freeSpace();

}
```


Este sistema, de acordo com a interface proposta, fará a operação de um sistema de arquivos
FAT32 armazenado arquivos em um único arquivo do sistema, como se este arquivo fosse uma
unidade de armazenamento secundária. Através das chamadas à interface será permitido que
os usuários operem o sistema de arquivos.

Observações:
* Deverá ser desenvolvido em linguagem Java utilizando a classe RandomAccessFile e a
chamada seek(pos) para busca e leitura/gravação de blocos inteiros;
* Os blocos serão de 64KB;
* O tamanho do arquivo para armazenamento poderá ser definido na inicialização (caso
o arquivo seja novo);

* O primeiro bloco será reservado para as informações do diretório (único), dentre as
quais:
o Nome do arquivo: tamanho fixo de 8 caracteres e três para a extensão.
o Tamanho total do arquivo: inteiro de 32 bits.
o Bloco Inicial: inteiro de 32 bits.

* Os próximos blocos serão usados para o armazenamento da FAT (calcular a
quantidade de blocos necessários de acordo com o tamanho do arquivo);

* Operações inválidas geram exceções como por exemplo: tentar ler um arquivo além
do seu tamanho, tentar gravar mais dados do que o espaço livre permite, etc.

* Deverão ser desenvolvidos casos de teste para cada uma das chamadas
implementadas;