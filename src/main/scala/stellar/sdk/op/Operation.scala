package stellar.sdk.op

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.json4s.{CustomSerializer, DefaultFormats}
import org.json4s.JsonAST.{JObject, JValue}
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{AccountID, Operation => XDROp}
import stellar.sdk._
import stellar.sdk.XDRPrimitives
import stellar.sdk.resp.OperationResp

import scala.util.{Success, Try}

trait Operation extends XDRPrimitives {
  def toOperationBody: OperationBody

  def toXDR: XDROp = {
    val op = new org.stellar.sdk.xdr.Operation()
//    val src = new AccountID()
//    src.setAccountID(sourceAccount.getXDRPublicKey)
//    op.setSourceAccount(src)
    op.setBody(toOperationBody)
    op
  }
}

object Operation {

  val ONE = BigDecimal(10).pow(7)

  def fromXDR(op: XDROp): Try[Operation] = {
    op.getBody.getDiscriminant match {
      case ALLOW_TRUST => AllowTrustOperation.from(op.getBody.getAllowTrustOp)
      case CHANGE_TRUST => ChangeTrustOperation.from(op.getBody.getChangeTrustOp)
      case CREATE_ACCOUNT => CreateAccountOperation.from(op.getBody.getCreateAccountOp)
      case PATH_PAYMENT => PathPaymentOperation.from(op.getBody.getPathPaymentOp)
      case PAYMENT => PaymentOperation.from(op.getBody.getPaymentOp)
      case SET_OPTIONS => SetOptionsOperation.from(op.getBody.getSetOptionsOp)
      case MANAGE_OFFER => ManageOfferOperation.from(op.getBody.getManageOfferOp)
      case CREATE_PASSIVE_OFFER => CreatePassiveOfferOperation.from(op.getBody.getCreatePassiveOfferOp)
      case ACCOUNT_MERGE => AccountMergeOperation.from(op.getBody)
      case INFLATION => Success(InflationOperation)
      case MANAGE_DATA => ManageDataOperation.from(op.getBody.getManageDataOp)
    }
  }
}

object OperationDeserializer extends CustomSerializer[Operation](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def asset(prefix: String = "", obj: JValue = o) = {
      def assetCode = (obj \ s"${prefix}asset_code").extract[String]
      def assetIssuer = KeyPair.fromAccountId((obj \ s"${prefix}asset_issuer").extract[String])
      (obj \ s"${prefix}asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => AssetTypeCreditAlphaNum4(assetCode, assetIssuer)
        case "credit_alphanum12" => AssetTypeCreditAlphaNum12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def price(label: String = "price_r"): Price = Price(
      n = (o \ label \ "n").extract[Int],
      d = (o \ label \ "d").extract[Int]
    )

    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def nativeAmount(key: String) = {
      NativeAmount(Amount.toBaseUnits(doubleFromString(key)).get)
    }

    def amount(label: String = "amount", assetPrefix: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString(label)).get
      asset(assetPrefix) match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case NativeAsset => NativeAmount(units)
      }
    }

    def date(key: String) = ZonedDateTime.from(formatter.parse((o \ key).extract[String]))
    //
    //    def weight = (o \ "weight").extract[Int].toShort

    (o \ "type").extract[String] match {
      //      case "create_account" =>
      //        OperationCreateAccount(id, txnHash, source, createdAt, account(), account("funder"), nativeAmount("starting_balance"))
      //      case "payment" =>
      //        OperationPayment(id, txnHash, source, createdAt, amount(), account("from"), account("to"))
      //      case "path_payment" =>
      //        val JArray(pathJs) = o \ "path"
      //        val path: List[Asset] = pathJs.map(a => asset(obj = a))
      //        OperationPathPayment(id, txnHash, source, createdAt, amount("source_max", "source_"), account("from"), amount(),
      //          account("to"), path)
      case "manage_offer" =>
        (o \ "offer_id").extract[Long] match {
          case 0L => CreateOfferOperation(
            selling = amount(assetPrefix = "selling_"),
            buying = asset("buying_"),
            price = price()
          )
        }




      //      case "account_created" =>
      //        val startingBalance = Amount.lumens((o \ "starting_balance").extract[String].toDouble).get
      //        EffectAccountCreated(id, account(), startingBalance)
      //      case "account_credited" => EffectAccountCredited(id, account(), amount())
      //      case "account_debited" => EffectAccountDebited(id, account(), amount())
      //      case "account_removed" => EffectAccountRemoved(id, account())
      //      case "account_thresholds_updated" =>
      //        val thresholds = Thresholds(
      //          (o \ "low_threshold").extract[Int],
      //          (o \ "med_threshold").extract[Int],
      //          (o \ "high_threshold").extract[Int]
      //        )
      //        EffectAccountThresholdsUpdated(id, account(), thresholds)
      //      case "account_home_domain_updated" => EffectAccountHomeDomainUpdated(id, account(), (o \ "home_domain").extract[String])
      //      case "account_flags_updated" => EffectAccountFlagsUpdated(id, account(), (o \ "auth_required_flag").extract[Boolean])
      //      case "signer_created" => EffectSignerCreated(id, account(), weight, (o \ "public_key").extract[String])
      //      case "signer_updated" => EffectSignerUpdated(id, account(), weight, (o \ "public_key").extract[String])
      //      case "signer_removed" => EffectSignerRemoved(id, account(), (o \ "public_key").extract[String])
      //      case "trustline_created" => EffectTrustLineCreated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
      //      case "trustline_updated" => EffectTrustLineUpdated(id, account(), asset().asInstanceOf[NonNativeAsset], doubleFromString("limit"))
      //      case "trustline_removed" => EffectTrustLineRemoved(id, account(), asset().asInstanceOf[NonNativeAsset])
      //      case "trustline_authorized" => EffectTrustLineAuthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
      //      case "trustline_deauthorized" => EffectTrustLineDeauthorized(id, account("trustor"), asset(issuerKey = "account").asInstanceOf[NonNativeAsset])
      //      case "trade" => EffectTrade(id, (o \ "offer_id").extract[Long], account(), amount("bought_"), account("seller"), amount("sold_"))
      case t =>
        // throw new RuntimeException(s"Unrecognised operation type '$t'")
        null
    }
}, PartialFunction.empty)
)

