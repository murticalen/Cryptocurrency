import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {
	
	private UTXOPool utxoPool;
	
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        return allOutputsClaimedInCurrentPool(tx) && validSignatures(tx)
        		&& noMultipleClaims(tx) && allOutputsNonNeg(tx)
        		&& sumInputGreaterThanOutput(tx);
    }
    
	private boolean allOutputsClaimedInCurrentPool(Transaction tx) {
		for(Transaction.Input in : tx.getInputs()) {
			if(!utxoPool.contains(new UTXO(in.prevTxHash, in.outputIndex))) {
				return false;
			}
		}
		return true;
	}

    private boolean validSignatures(Transaction tx) {
    		for(int i = 0; i < tx.numInputs(); i++) {
    			Transaction.Input in = tx.getInput(i);
    			Transaction.Output out = utxoPool.getTxOutput(new UTXO(in.prevTxHash, in.outputIndex));
    			if(!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature)) {
    				return false;
    			}
    		}
    		return true;
	}
    
	private boolean noMultipleClaims(Transaction tx) {
		Set<UTXO> set = new HashSet<>();
		for(Transaction.Input in : tx.getInputs()) {
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			if(set.contains(utxo)) {
				return false;
			}
			set.add(utxo);
		}
		return true;
	}

	private boolean allOutputsNonNeg(Transaction tx) {
		for(Transaction.Output out : tx.getOutputs()) {
			if(Double.compare(out.value, 0.0) < 0) {
				return false;
			}
		}
		return true;
	}

	private boolean sumInputGreaterThanOutput(Transaction tx) {
		double inputSum = 0;
		for(Transaction.Input in : tx.getInputs()) {
			inputSum += utxoPool.getTxOutput(new UTXO(in.prevTxHash, in.outputIndex)).value;
		}
		double outputSum = 0;
		for(Transaction.Output out : tx.getOutputs()) {
			outputSum += out.value;
		}
		return Double.compare(inputSum, outputSum) > 0;
	}

	/**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> list = new ArrayList<>();
        for(Transaction tx : possibleTxs) {
        		if(isValidTx(tx)) {
        			list.add(tx);
        			for(Transaction.Input in : tx.getInputs()) {
        				utxoPool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
        			}
        			for(int i = 0; i < tx.numOutputs(); i++) {
        				utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
        			}
        		}
        }
        return list.stream().toArray(Transaction[]::new);
    }

}
