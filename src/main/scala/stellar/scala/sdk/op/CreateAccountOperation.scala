package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr._
import stellar.scala.sdk._

import scala.util.Try

/**
  * Represents <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html#create-account" target="_blank">CreateAccount</a> operation.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/list-of-operations.html" target="_blank">List of Operations</a>
  */
case class CreateAccountOperation(destinationAccount: PublicKeyOps,
                                  startingBalance: NativeAmount = NativeAmount(0),
                                  sourceAccount: Option[KeyPair] = None) extends Operation {

  override def toOperationBody: OperationBody = {
    val op = new CreateAccountOp()
    val destination = new AccountID()
    destination.setAccountID(destinationAccount.getXDRPublicKey)
    op.setDestination(destination)
    val startBal = new Int64()
    startBal.setInt64(startingBalance.units)
    op.setStartingBalance(startBal)
    val body = new org.stellar.sdk.xdr.Operation.OperationBody()
    body.setDiscriminant(OperationType.CREATE_ACCOUNT)
    body.setCreateAccountOp(op)
    body
  }

}

object CreateAccountOperation {

  def apply(sourceAccount: KeyPair,
            destinationAccount: PublicKeyOps,
            startingBalance: NativeAmount): CreateAccountOperation = {
    CreateAccountOperation(destinationAccount, startingBalance, Some(sourceAccount))
  }

  def from(op: CreateAccountOp): Try[CreateAccountOperation] = Try {
    CreateAccountOperation(
      sourceAccount = None,
      destinationAccount = KeyPair.fromPublicKey(op.getDestination.getAccountID.getEd25519.getUint256),
      startingBalance = NativeAmount(op.getStartingBalance.getInt64.longValue)
    )
  }
}