package com.argus.rag.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** QA 闂瓟璁板綍瀹炰綋锛屽搴?qa_records 琛ㄣ€?*/
@Data
@TableName("qa_records")
public class QaRecordEntity {

    /** 闂瓟璁板綍涓婚敭銆?*/
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 鎻愰棶鐢ㄦ埛 ID銆?*/
    private Long userId;
    /** 闂瓟鍙戠敓鐨勭煡璇嗗簱缇ょ粍 ID銆?*/
    private Long groupId;
    /** 鐢ㄦ埛鍘熷闂銆?*/
    private String question;
    /** 妯″瀷鐢熸垚鐨勫洖绛旀鏂囷紝鎷掔瓟鎴栧け璐ユ椂鍙负绌恒€?*/
    private String answer;
    /** 鏄惁鎴愬姛鍥炵瓟浜嗛棶棰樸€?*/
    private Boolean answered;
    /** 鎷掔瓟鎴栧け璐ュ師鍥犵紪鐮併€?*/
    private String reasonCode;
    /** 鎷掔瓟鎴栧け璐ュ師鍥犺鏄庛€?*/
    private String reasonMessage;
    /** 璇佹嵁鍏呭垎鎬х瓑绾с€?*/
    private String evidenceLevel;
    /** 淇濆瓨鐨勫紩鐢ㄥ揩鐓ф暟閲忋€?*/
    private Integer citationCount;
    /** 杈撳叆 token 鏁般€?*/
    private Integer promptTokens;
    /** 杈撳嚭 token 鏁般€?*/
    private Integer completionTokens;
    /** 鎬?token 鏁般€?*/
    private Integer totalTokens;
    /** token 鐢ㄩ噺鏄惁涓轰及绠楀€笺€?*/
    private Boolean isEstimated;
    /** 鏈闂瓟鑰楁椂锛屽崟浣嶆绉掋€?*/
    private Long latencyMs;
    /** 璋冪敤鐨勫ぇ妯″瀷鍚嶇О銆?*/
    private String modelName;
    /** 瑙﹀彂璁板綍鐨勬帴鍙ｇ鐐广€?*/
    private String endpoint;
    /** 鎸佷箙鍖栧搴旀祦绋嬫槸鍚︽垚鍔熷畬鎴愩€?*/
    private Boolean success;
    /** 绯荤粺寮傚父淇℃伅锛屼笟鍔℃嫆绛旀椂閫氬父涓虹┖銆?*/
    private String errorMessage;
    /** 璁板綍鍒涘缓鏃堕棿銆?*/
    private LocalDateTime createdAt;
    /** Soft delete timestamp; null means active. */
    private LocalDateTime deletedAt;
    /** User who soft-deleted this record. */
    private Long deletedBy;
    /** Optional soft-delete reason. */
    private String deleteReason;
}