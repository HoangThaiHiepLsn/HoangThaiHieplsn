import java.security.Security;
import java.util.ArrayList;
//import java.util.Base64;
import java.util.HashMap;
//import com.google.gson.GsonBuilder;
import java.util.Scanner;

public class HTH_Blockchain {

    public static ArrayList<VNPT_Hiep> blockchain = new ArrayList<VNPT_Hiep>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 4;
    public static float minimumTransaction = 0.1f;
    public static Mobiphone mobiphone1; //Kho điện thoại thứ 1
    public static Mobiphone mobiphone2; //Kho điện thoại thứ 2
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        Scanner BlockData = new Scanner(System.in);
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider

        //Create wallets:
        mobiphone1 = new Mobiphone();
        mobiphone2 = new Mobiphone();
        Mobiphone mobiphonebase = new Mobiphone();
        // Nhập từ bàn phím

        System.out.println("Yêu cầu nhập số lượng điện thoại trong kho thứ 1: ");
        int x = Integer.parseInt(BlockData.nextLine());

        //Khởi tạo số lượng điện thoại trong kho thứ 1

        genesisTransaction = new Transaction(mobiphonebase.publicKey, mobiphone1.publicKey, x, null);
        genesisTransaction.generateSignature(mobiphonebase.privateKey);//Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Yêu cầu nhập số lượng điện thoại trong kho thứ 2: ");
        int y = Integer.parseInt(BlockData.nextLine());

        //Khởi tạo số lượng điện thoại trong kho thứ 2

        Transaction genesisTransaction2 = new Transaction(mobiphonebase.publicKey, mobiphone2.publicKey, y, null);
        genesisTransaction2.generateSignature(mobiphonebase.privateKey);	 //Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction2.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction2.outputs.add(new TransactionOutput(genesisTransaction2.reciepient, genesisTransaction2.value, genesisTransaction2.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction2.outputs.get(0).id, genesisTransaction2.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Đang tạo và đào khối gốc .... ");
        VNPT_Hiep genesis = new VNPT_Hiep("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        System.out.println("\nSố dư điện thoại trong kho thứ 1 là : " + mobiphone1.getBalance());
        System.out.println("\nSố dư điện thoại trong kho thứ 2 là : " + mobiphone2.getBalance());

        //Chạy chương trình chuyển điện thoại
        VNPT_Hiep block1 = new VNPT_Hiep(genesis.hash);
        System.out.println("\nGiao dịch số lượng điện thoại từ kho thứ 1 đến kho thứ 2 là: ");
        int z = Integer.parseInt(BlockData.nextLine());
        block1.addTransaction(mobiphone1.sendFunds(mobiphone2.publicKey, z));
        addBlock(block1);

        System.out.println("\nSố dư mới điện thoại trong kho thứ 1 là : " + mobiphone1.getBalance());
        System.out.println("Số dư mới điện thoại trong kho thứ 2 là : " + mobiphone2.getBalance());


    isChainValid();
    }

    public static Boolean isChainValid() {
        VNPT_Hiep currentBlock;
        VNPT_Hiep previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t = 0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }

            }

        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_Hiep newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}

