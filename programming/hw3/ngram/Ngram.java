public class Ngram{
    public static void main(String[] args){
        try{
            int ngram_length = Integer.parseInt(args[1]);
            int slide = Integer.parseInt(args[2]);
        }catch(Exception e){
            System.err.println("Input arguments not proper");
            System.exit(1);
        }
        String fileName = args[3];
        String out = args[4];
    }
}
