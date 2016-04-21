import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

class Ngram{
    public static void main(String[] args){
        int ngramLength=0, slide=0;
        try{
            ngramLength = Integer.parseInt(args[0]);
            slide = Integer.parseInt(args[1]);
            if(slide > ngramLength)
                throw new NumberFormatException();
        }catch(NumberFormatException e){
            System.err.println("Input arguments are not proper. Please check.");
            System.exit(1);
        }

        try(
            RandomAccessFile in = new RandomAccessFile(args[2], "r");
            FileOutputStream out = new FileOutputStream(args[3])
        ){
            Map<String, Integer> ngrams = doAnalysis(ngramLength, slide, in);
            report(ngrams, out);
        } catch (IOException e) {
            System.out.println("Cannot Read/Write from files. Please check arguments and/or permissions." + e.toString());
            System.exit(1);
        }
        System.out.println("Ngram Analysis complete");
    }

    private static Map<String, Integer> doAnalysis(int ngramLength, int slide, RandomAccessFile in) throws IOException {
        /*
         * Does the ngram analysis. Creates a key Value map of ngram bytes to their counts.
         *
         * @param ngramLength: value of n for the analysis
         * @param slide: length of the slide used in the analysis
         * @param in: RandomAccessStream from the input binary file on which the analysis is to be performed.
         *
         * @return Map of ngram bytes to their count.
         *
         */
        Map<String, Integer> ngrams = new LinkedHashMap<>();
        byte[] gram = new byte[ngramLength];
        for(int x=ngramLength;  x==ngramLength; in.seek(in.getFilePointer()+slide-ngramLength)){
            // marks a position in the stream reads the number of characters required and then moves
            // a few positions described by the slide window length.
            x = in.read(gram);
            if(x<ngramLength)
                break;
            String gramString = binaryToHex(gram, x);
            if(ngrams.containsKey(gramString)){
                ngrams.put(gramString, ngrams.get(gramString)+1);
            }else{
                ngrams.put(gramString, 1);
            }
        }
        in.close();
        return ngrams;
    }

    private static void report(Map<String, Integer> ngrams, FileOutputStream out) throws IOException {
        /*
         * Sorts the output of the ngram analysis by counts and report them in an output file.
         *
         * @param ngrams: map of ngram bytes to their counts
         * @param out: output stream to the file in which result is to be saved
         */
        List<Map.Entry<String, Integer>> ngramsList = new ArrayList<>();
        for(Map.Entry<String, Integer> ngram : ngrams.entrySet()){
            ngramsList.add(ngram);
        }
        Collections.sort(ngramsList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                if(!o1.getValue().equals(o2.getValue())){
                    return o2.getValue().compareTo(o1.getValue());
                }
                Long a = Long.parseLong(o1.getKey(), 16);
                return a.compareTo(Long.parseLong(o2.getKey(), 16));
            }
        });
        for(Map.Entry<String, Integer> ngram : ngramsList){
            out.write((ngram.getKey() + " " + ngram.getValue() + "\n").getBytes());
        }
    }

    private static String binaryToHex(byte[] bytes, int x) {
        /*
         * Converts the binary values to hex according to the format the strings are to be reported.
         */
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
