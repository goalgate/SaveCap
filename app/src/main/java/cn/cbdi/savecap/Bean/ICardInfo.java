package cn.cbdi.savecap.Bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class ICardInfo {
    @Id
    String cardId;

    String name;

    @Generated(hash = 410267148)
    public ICardInfo(String cardId, String name) {
        this.cardId = cardId;
        this.name = name;
    }

    @Generated(hash = 456590415)
    public ICardInfo() {
    }

    public String getCardId() {
        return this.cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }



    

}
