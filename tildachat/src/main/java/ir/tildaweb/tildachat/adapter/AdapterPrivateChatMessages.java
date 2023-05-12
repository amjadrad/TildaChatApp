package ir.tildaweb.tildachat.adapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import ir.tildaweb.tildachat.R;
import ir.tildaweb.tildachat.app.TildaChatApp;
import ir.tildaweb.tildachat.dialogs.DialogShowPicture;
import ir.tildaweb.tildachat.enums.ChatroomType;
import ir.tildaweb.tildachat.interfaces.IChatUtils;
import ir.tildaweb.tildachat.interfaces.LoadMoreData;
import ir.tildaweb.tildachat.models.base_models.Message;
import ir.tildaweb.tildachat.ui.values.MessageTypeUtil;
import ir.tildaweb.tildachat.utils.DateUtils;
import ir.tildaweb.tildachat.utils.FileDownloaderNew;
import ir.tildaweb.tildachat.utils.OnSwipeTouchListener;


public class AdapterPrivateChatMessages extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FileDownloaderNew.OnFileDownloadListener {

    public enum SearchType {
        REPLY,
    }

    private final String TAG = getClass().getName();
    private final Context context;
    private final Activity activity;
    private final ArrayList<Message> chatMessages;
    private final RecyclerView recyclerView;
    private final int userId;
    private final DateUtils dateHelper;
    private ChatroomType roomType = ChatroomType.PRIVATE;
    private final String FILE_URL;
    private boolean isAdmin;

    private final int visibleThreshold = 5;
    private int firstVisibleItem;
    private boolean loading = true;
    private final LoadMoreData loadMoreData;
    private final IChatUtils iChatUtils;
    private DownloadManager downloadManager;
    private boolean isDownloadingFile = false;

    public AdapterPrivateChatMessages(Context context, Activity activity, int userId, String FILE_URL, RecyclerView recyclerView, ArrayList<Message> chatMessages, LoadMoreData loadMoreData, IChatUtils iChatUtils) {
        this.chatMessages = chatMessages;
        this.context = context;
        this.activity = activity;
        this.userId = userId;
        this.iChatUtils = iChatUtils;
        this.recyclerView = recyclerView;
        this.loadMoreData = loadMoreData;
        this.dateHelper = new DateUtils();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (FILE_URL.endsWith("/")) {
            this.FILE_URL = FILE_URL;
        } else {
            this.FILE_URL = FILE_URL.concat("/");
        }
        setScrollListener();
    }

    private void setScrollListener() {
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    firstVisibleItem = linearLayoutManager
                            .findFirstVisibleItemPosition();
                    if (!loading && (firstVisibleItem - visibleThreshold) < 0) {
                        if (loadMoreData != null) {
                            loading = true;
                            loadMoreData.onLoadMore();
                        }
                    }
                }
            });
        }
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public Holder(View view) {
            super(view);
        }
    }

    //Upload
    public static class ChatHolder_Upload extends Holder {
//        private final TextView tvPercent;
//        private final TextView tvMessage;

        public ChatHolder_Upload(View view) {
            super(view);
//            this.tvMessage = view.findViewById(R.id.tvMessage);
//            this.tvPercent = view.findViewById(R.id.tvPercent);
        }
    }

    //Text
    public static class ChatHolder_Text_ReplyFalse_Me_Private extends Holder {
        public TextView tvTime;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearMessage;

        public ChatHolder_Text_ReplyFalse_Me_Private(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
        }
    }

    public static class ChatHolder_Text_ReplyFalse_Me_Group extends Holder {
        public TextView tvTime;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearMessage;

        public ChatHolder_Text_ReplyFalse_Me_Group(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
        }
    }

    public static class ChatHolder_Text_ReplyFalse_Other_Private extends Holder {
        public TextView tvTime;
        public TextView tvMessage;

        public ChatHolder_Text_ReplyFalse_Other_Private(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
        }
    }

    public static class ChatHolder_Text_ReplyTrue_Me_Private extends Holder {
        public TextView tvTime, tvReply;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;


        public ChatHolder_Text_ReplyTrue_Me_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);

        }
    }

    public static class ChatHolder_Text_ReplyTrue_Me_Group extends Holder {
        public TextView tvTime, tvReply;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;


        public ChatHolder_Text_ReplyTrue_Me_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);

        }
    }

    public static class ChatHolder_Text_ReplyTrue_Other_Private extends Holder {
        public TextView tvTime, tvReply;
        public TextView tvMessage;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;


        public ChatHolder_Text_ReplyTrue_Other_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }

    public static class ChatHolder_Text_ReplyFalse_Other_Group extends Holder {
        public TextView tvTime;
        public TextView tvMessage;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;


        public ChatHolder_Text_ReplyFalse_Other_Group(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
        }
    }

    public static class ChatHolder_Text_ReplyTrue_Other_Group extends Holder {
        public TextView tvTime;
        public TextView tvMessage;
        public TextView tvReply;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;


        public ChatHolder_Text_ReplyTrue_Other_Group(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }


    //Picture
    public static class ChatHolder_Picture_ReplyFalse_Me_Private extends Holder {
        public TextView tvTime;
        public ImageView imageViewSeen;
        public ImageView imageView;
        public LinearLayout linearMessage;

        public ChatHolder_Picture_ReplyFalse_Me_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
        }
    }

    public static class ChatHolder_Picture_ReplyTrue_Me_Private extends Holder {
        public TextView tvTime;
        public ImageView imageViewSeen;
        public ImageView imageView;
        public LinearLayout linearMessage;
        public TextView tvReply;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;

        public ChatHolder_Picture_ReplyTrue_Me_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }

    public static class ChatHolder_Picture_ReplyFalse_Me_Group extends Holder {
        public TextView tvTime;
        public ImageView imageViewSeen;
        public ImageView imageView;
        public LinearLayout linearMessage;

        public ChatHolder_Picture_ReplyFalse_Me_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
        }
    }

    public static class ChatHolder_Picture_ReplyTrue_Me_Group extends Holder {
        public TextView tvTime;
        public ImageView imageViewSeen;
        public ImageView imageView;
        public LinearLayout linearMessage;
        public TextView tvReply;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;

        public ChatHolder_Picture_ReplyTrue_Me_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }

    public static class ChatHolder_Picture_ReplyFalse_Other_Private extends Holder {
        public TextView tvTime;
        public ImageView imageView;
        public LinearLayout linearMessage;

        public ChatHolder_Picture_ReplyFalse_Other_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
        }
    }

    public static class ChatHolder_Picture_ReplyFalse_Other_Group extends Holder {
        public TextView tvTime;
        public ImageView imageView;
        public LinearLayout linearMessage;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;


        public ChatHolder_Picture_ReplyFalse_Other_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
        }
    }

    public static class ChatHolder_Picture_ReplyTrue_Other_Private extends Holder {
        public TextView tvTime;
        public ImageView imageView;
        public LinearLayout linearMessage;
        public TextView tvReply;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;

        public ChatHolder_Picture_ReplyTrue_Other_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }

    public static class ChatHolder_Picture_ReplyTrue_Other_Group extends Holder {
        public TextView tvTime;
        public ImageView imageView;
        public LinearLayout linearMessage;
        public TextView tvReply;
        public LinearLayout linearLayoutReply;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;

        public ChatHolder_Picture_ReplyTrue_Other_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageView = view.findViewById(R.id.imageView);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
        }
    }


    //File
    public static class ChatHolder_File_ReplyFalse_Me_Private extends Holder {
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        private final TextView tvMessage;
        private final ImageView imageViewSeen;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;

        public ChatHolder_File_ReplyFalse_Me_Private(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }

    public static class ChatHolder_File_ReplyFalse_Me_Group extends Holder {
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearMessage;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;

        public ChatHolder_File_ReplyFalse_Me_Group(View view) {
            super(view);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.linearMessage = view.findViewById(R.id.linearChatMessage);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }

    public static class ChatHolder_File_ReplyFalse_Other_Private extends Holder {
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;

        public ChatHolder_File_ReplyFalse_Other_Private(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }

    public static class ChatHolder_File_ReplyTrue_Me_Private extends Holder {
        public TextView tvReply;
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;


        public ChatHolder_File_ReplyTrue_Me_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);

        }
    }

    public static class ChatHolder_File_ReplyTrue_Me_Group extends Holder {
        public TextView tvReply;
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        public ImageView imageViewSeen;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;


        public ChatHolder_File_ReplyTrue_Me_Group(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.imageViewSeen = view.findViewById(R.id.imageViewSeen);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);

        }
    }

    public static class ChatHolder_File_ReplyTrue_Other_Private extends Holder {
        public TextView tvReply;
        public TextView tvMessage;
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;


        public ChatHolder_File_ReplyTrue_Other_Private(View view) {
            super(view);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }

    public static class ChatHolder_File_ReplyFalse_Other_Group extends Holder {
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;


        public ChatHolder_File_ReplyFalse_Other_Group(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }

    public static class ChatHolder_File_ReplyTrue_Other_Group extends Holder {
        private final AppCompatTextView tvTime;
        private final AppCompatTextView tvProgress;
        public TextView tvMessage;
        public TextView tvReply;
        public TextView tvUserName;
        public CircleImageView imageViewProfile;
        public LinearLayout linearLayoutReply;
        public AppCompatImageView imageViewReplyMessage;
        public CardView cardViewReplyPicture;
        private final CoordinatorLayout coordinatorDownloadFile;
        private final CoordinatorLayout coordinatorDownloadedFile;


        public ChatHolder_File_ReplyTrue_Other_Group(View view) {
            super(view);
            this.tvMessage = view.findViewById(R.id.tvMessage);
            this.tvTime = view.findViewById(R.id.tvTime);
            this.tvProgress = view.findViewById(R.id.tvProgress);
            this.tvUserName = view.findViewById(R.id.tvUserName);
            this.imageViewProfile = view.findViewById(R.id.imageViewProfile);
            this.tvReply = view.findViewById(R.id.tvReplyMessage);
            this.linearLayoutReply = view.findViewById(R.id.linearLayoutReply);
            this.imageViewReplyMessage = view.findViewById(R.id.imageViewReplyMessage);
            this.cardViewReplyPicture = view.findViewById(R.id.cardViewReplyPicture);
            this.coordinatorDownloadFile = view.findViewById(R.id.coordinatorDownloadFile);
            this.coordinatorDownloadedFile = view.findViewById(R.id.coordinatorDownloadedFile);
        }
    }


    @Override
    public int getItemViewType(int position) {
        Message chatMessage = chatMessages.get(position);
        int type = MessageTypeUtil.getType(chatMessage, userId, roomType);
//        Log.d(TAG, "getItemViewType: " + type + " _ " + chatMessage.getMessage() + " _ " + chatMessage.getUserId());
        return type;
    }


    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        Message chatMessage = chatMessages.get(holder.getAdapterPosition());
        if (chatMessage.getUserId() != userId) {
            if (chatMessage.getSeenCount() == 0) {
                iChatUtils.onMessageSeen(chatMessage.getId());
            }
        }
    }

    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //Message type (text , picture , file , voice)
        //Reply (false, true)
        //User type (me , other)
        //Chatroom type (private , channel, group)

        //MRUC0(4,2,2,3,0)

        switch (viewType) {
            case 1:
                return new ChatHolder_Upload(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_upload, parent, false));
            case 11111:
                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
//            case 11121:
//                return null;
            case 11131:
                return new ChatHolder_Text_ReplyFalse_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_group, parent, false));
            case 11211:
                return new ChatHolder_Text_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_other_private, parent, false));
            case 11221://Channel,todo
                return new ChatHolder_Text_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_other_private, parent, false));
            case 11231:
                return new ChatHolder_Text_ReplyFalse_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_other_group, parent, false));
            case 12111:
                return new ChatHolder_Text_ReplyTrue_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replytrue_me_private, parent, false));
//            case 12121://Channel
//                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
            case 12131:
                return new ChatHolder_Text_ReplyTrue_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replytrue_me_group, parent, false));
            case 12211:
                return new ChatHolder_Text_ReplyTrue_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replytrue_other_private, parent, false));
//            case 12221:
//                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
            case 12231:
                return new ChatHolder_Text_ReplyTrue_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replytrue_other_group, parent, false));

            case 21111:
                return new ChatHolder_Picture_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replyfalse_me_private, parent, false));
//            case 21121:
//                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
            case 21131:
                return new ChatHolder_Picture_ReplyFalse_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replyfalse_me_group, parent, false));
            case 21211:
                return new ChatHolder_Picture_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replyfalse_other_private, parent, false));
            case 21221://channel todo
                return new ChatHolder_Picture_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replyfalse_other_private, parent, false));
            case 21231:
                return new ChatHolder_Picture_ReplyFalse_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replyfalse_other_group, parent, false));
            case 22111:
                return new ChatHolder_Picture_ReplyTrue_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replytrue_me_private, parent, false));
////            case 22121:
////                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
            case 22131:
                return new ChatHolder_Picture_ReplyTrue_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replytrue_me_group, parent, false));
            case 22211:
                return new ChatHolder_Picture_ReplyTrue_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replytrue_other_private, parent, false));
////            case 22221:
////                return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
            case 22231:
                return new ChatHolder_Picture_ReplyTrue_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_picture_replytrue_other_group, parent, false));

            //File

            case 31111:
                return new ChatHolder_File_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replyfalse_me_private, parent, false));
            case 31131:
                return new ChatHolder_File_ReplyFalse_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replyfalse_me_group, parent, false));
            case 31211:
                return new ChatHolder_File_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replyfalse_other_private, parent, false));
            case 31231:
                return new ChatHolder_File_ReplyFalse_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replyfalse_other_group, parent, false));
            case 32111:
                return new ChatHolder_File_ReplyTrue_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replytrue_me_private, parent, false));
            case 32131:
                return new ChatHolder_File_ReplyTrue_Me_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replytrue_me_group, parent, false));
            case 32211:
                return new ChatHolder_File_ReplyTrue_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replytrue_other_private, parent, false));
            case 32231:
                return new ChatHolder_File_ReplyTrue_Other_Group(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replytrue_other_group, parent, false));
            case 31221://channel todo
                return new ChatHolder_File_ReplyFalse_Other_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_file_replyfalse_other_private, parent, false));

        }

        return new ChatHolder_Text_ReplyFalse_Me_Private(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_socket_chat_text_replyfalse_me_private, parent, false));
    }

    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {

        final Message chatMessage = chatMessages.get(position);

        //Message type (text , picture , file , voice)
        //Reply (false, true)
        //User type (me , other)
        //Chatroom type (private , channel, group)

        //MRUC0(4,2,2,3,0)

        viewHolder.itemView.setOnTouchListener(new OnSwipeTouchListener(context) {
            public void onSwipeTop() {
//                Log.d(TAG, "onSwipeTop: ");
            }

            public void onSwipeRight() {
//                Log.d(TAG, "onSwipeRight: ");
            }

            public void onSwipeLeft() {
//                Log.d(TAG, "onSwipeLeft: ");
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(50);
                }
                iChatUtils.onReply(chatMessage);
            }

            public void onSwipeBottom() {
//                Log.d(TAG, "onSwipeBottom: ");
            }

        });

        switch (viewHolder.getItemViewType()) {

            case 1: {
//                ChatHolder_Upload holder = (ChatHolder_Upload) viewHolder;
//                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
//                holder.tvPercent.setText(String.format("%s%s", chatMessage.getPercent(), "%"));

                break;
            }
            case 11111: {
                ChatHolder_Text_ReplyFalse_Me_Private holder = (ChatHolder_Text_ReplyFalse_Me_Private) viewHolder;

                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                holder.itemView.setOnClickListener(view -> {

                    // copy , reply , delete
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me_text, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.edit) {
                            iChatUtils.onEdit(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });
                break;
            }
//                         case 11121:
//                return null;
//            ChatHolder_Text_ReplyFalse_Me_Group
            case 11131: {
                ChatHolder_Text_ReplyFalse_Me_Group holder = (ChatHolder_Text_ReplyFalse_Me_Group) viewHolder;

                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                holder.itemView.setOnClickListener(view -> {

                    // copy , reply , delete
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me_text, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.edit) {
                            iChatUtils.onEdit(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });
                break;
            }


//            ChatHolder_Text_ReplyFalse_Other_Private
            case 11211: {
                ChatHolder_Text_ReplyFalse_Other_Private holder = (ChatHolder_Text_ReplyFalse_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });


                break;
            }
            //Channel text
            case 11221: {
                ChatHolder_Text_ReplyFalse_Other_Private holder = (ChatHolder_Text_ReplyFalse_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                holder.itemView.setOnClickListener(view -> {
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (isAdmin) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_channel_other_text, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        } else if (itemId == R.id.edit) {
                            iChatUtils.onEdit(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }

//            case 11221:
//                return null;
//            ChatHolder_Text_ReplyFalse_Other_Group
            case 11231: {
                ChatHolder_Text_ReplyFalse_Other_Group holder = (ChatHolder_Text_ReplyFalse_Other_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));

                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });
                break;
            }
//            ChatHolder_Text_ReplyTrue_Me_Private
            case 12111: {
                ChatHolder_Text_ReplyTrue_Me_Private holder = (ChatHolder_Text_ReplyTrue_Me_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.edit) {
                            iChatUtils.onEdit(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));
                break;
            }
//            case 12121:
//                return null;
//            ChatHolder_Text_ReplyTrue_Me_Group
            case 12131: {
                ChatHolder_Text_ReplyTrue_Me_Group holder = (ChatHolder_Text_ReplyTrue_Me_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.edit) {
                            iChatUtils.onEdit(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));
                break;
            }

//            ChatHolder_Text_ReplyTrue_Other_Private
            case 12211: {
                ChatHolder_Text_ReplyTrue_Other_Private holder = (ChatHolder_Text_ReplyTrue_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                holder.itemView.setOnClickListener(view -> {
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });
                break;

            }
//            case 12221:
//
//            ChatHolder_Text_ReplyTrue_Other_Group
            case 12231: {
                ChatHolder_Text_ReplyTrue_Other_Group holder = (ChatHolder_Text_ReplyTrue_Other_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));

                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));
                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.copy) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("متن", holder.tvMessage.getText().toString());
                            clipboard.setPrimaryClip(clip);
                            iChatUtils.onCopy();
                        } else if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }

//            ChatHolder_Picture_ReplyFalse_Me_Private
            case 21111: {

                final ChatHolder_Picture_ReplyFalse_Me_Private holder = (ChatHolder_Picture_ReplyFalse_Me_Private) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });
                });

//                Log.d(TAG, "onBindViewHolder: " + FILE_URL + chatMessage.getMessage());
                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);
                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }
//            case 21121:
//
//            ChatHolder_Picture_ReplyFalse_Me_Group
            case 21131: {
                final ChatHolder_Picture_ReplyFalse_Me_Group holder = (ChatHolder_Picture_ReplyFalse_Me_Group) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }

//            ChatHolder_Picture_ReplyFalse_Other_Private
            case 21211: {
                final ChatHolder_Picture_ReplyFalse_Other_Private holder = (ChatHolder_Picture_ReplyFalse_Other_Private) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));


                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }
            //channel picture
            case 21221: {
                final ChatHolder_Picture_ReplyFalse_Other_Private holder = (ChatHolder_Picture_ReplyFalse_Other_Private) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));
                holder.itemView.setOnClickListener(view -> {
                    if (isAdmin) {
                        PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                        popup.show();
                        popup.setOnMenuItemClickListener(item -> {
                            int itemId = item.getItemId();
                            if (itemId == R.id.reply) {
                                iChatUtils.onReply(chatMessage);
                            } else if (itemId == R.id.delete) {
                                iChatUtils.onDelete(chatMessage);
                            }
                            return false;
                        });
                    }
                });
                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);
                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }
//            case 21221:
//
//            ChatHolder_Picture_ReplyFalse_Other_Group
            case 21231: {
                final ChatHolder_Picture_ReplyFalse_Other_Group holder = (ChatHolder_Picture_ReplyFalse_Other_Group) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));


                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);


                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }

//            ChatHolder_Picture_ReplyTrue_Me_Private
            case 22111: {
                final ChatHolder_Picture_ReplyTrue_Me_Private holder = (ChatHolder_Picture_ReplyTrue_Me_Private) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }

//            case 22121:
//
//            ChatHolder_Picture_ReplyTrue_Me_Group
            case 22131: {
                final ChatHolder_Picture_ReplyTrue_Me_Group holder = (ChatHolder_Picture_ReplyTrue_Me_Group) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });


                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }

//            ChatHolder_Picture_ReplyTrue_Other_Private
            case 22211: {
                final ChatHolder_Picture_ReplyTrue_Other_Private holder = (ChatHolder_Picture_ReplyTrue_Other_Private) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));

                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }

//            case 22221:
//
//            ChatHolder_Picture_ReplyTrue_Other_Group
            case 22231: {
                final ChatHolder_Picture_ReplyTrue_Other_Group holder = (ChatHolder_Picture_ReplyTrue_Other_Group) viewHolder;
                String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                holder.tvTime.setText(getTime(dateObject));
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));


                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));


                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);


                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    if (chatMessage.getMessageType().equals("text")) {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other_text, popup.getMenu());
                    } else {
                        inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    }
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });

                Glide.with(context).load(FILE_URL + chatMessage.getMessage()).into(holder.imageView);

                holder.imageView.setOnClickListener(view -> new DialogShowPicture(activity, FILE_URL, chatMessage.getMessage()).show());
                break;
            }
            //File
            case 31111: {
                ChatHolder_File_ReplyFalse_Me_Private holder = (ChatHolder_File_ReplyFalse_Me_Private) viewHolder;

                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage()));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            //Download file
                            downloadFile(chatMessage);
//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }

            case 31131: {
                ChatHolder_File_ReplyFalse_Me_Group holder = (ChatHolder_File_ReplyFalse_Me_Group) viewHolder;

                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                        FileDownloaderNew.openFile(context, chatMessage.getMessage());
                    } else {
                        downloadFile(chatMessage);
//                        FileDownloader fileDownloader = new FileDownloader();
//                        fileDownloader.setOnFileDownloadListener(() -> {
//                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                            notifyItemChanged(position);
//                        });
//                        fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                    }
                });

                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }
            case 31211: {
                ChatHolder_File_ReplyFalse_Other_Private holder = (ChatHolder_File_ReplyFalse_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {

                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);

//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });


                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }
            //channel file
            case 31221: {
                ChatHolder_File_ReplyFalse_Other_Private holder = (ChatHolder_File_ReplyFalse_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);

//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });
                holder.itemView.setOnClickListener(view -> {
                    if (isAdmin) {
                        PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                        popup.show();
                        popup.setOnMenuItemClickListener(item -> {
                            int itemId = item.getItemId();
                            if (itemId == R.id.reply) {
                                iChatUtils.onReply(chatMessage);
                            } else if (itemId == R.id.delete) {
                                iChatUtils.onDelete(chatMessage);
                            }
                            return false;
                        });
                    } else {
                        if (checkReadExternalPermission(activity)) {
                            if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
                            } else {
                                downloadFile(chatMessage);

//                                FileDownloader fileDownloader = new FileDownloader();
//                                fileDownloader.setOnFileDownloadListener(() -> {
//                                    FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                    notifyItemChanged(position);
//                                });
//                                fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                            }
                        }
                    }
                });
                break;
            }

            case 31231: {
                ChatHolder_File_ReplyFalse_Other_Group holder = (ChatHolder_File_ReplyFalse_Other_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));

                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);

//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });


                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });


                });
                break;
            }

            case 32111: {
                ChatHolder_File_ReplyTrue_Me_Private holder = (ChatHolder_File_ReplyTrue_Me_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });
                });

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);

//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });
                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));
                break;
            }

            case 32131: {
                ChatHolder_File_ReplyTrue_Me_Group holder = (ChatHolder_File_ReplyTrue_Me_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }
                if (chatMessage.getSeenCount() != 0) {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_double_check));
                } else {
                    holder.imageViewSeen.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_chat_single_check));
                }
                holder.itemView.setOnClickListener(view -> {

                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_me, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        } else if (itemId == R.id.delete) {
                            iChatUtils.onDelete(chatMessage);
                        }
                        return false;
                    });
                });

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);

//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });

                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));
                break;
            }


            case 32211: {
                ChatHolder_File_ReplyTrue_Other_Private holder = (ChatHolder_File_ReplyTrue_Other_Private) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }

                if (chatMessage.getReply().getMessageType().equals("text")) {
                    holder.tvReply.setVisibility(View.VISIBLE);
                    holder.cardViewReplyPicture.setVisibility(View.GONE);
                } else if (chatMessage.getReply().getMessageType().equals("picture")) {
                    holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                    holder.tvReply.setVisibility(View.GONE);
                    Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                } else {
                    holder.tvReply.setVisibility(View.VISIBLE);
                    holder.cardViewReplyPicture.setVisibility(View.GONE);
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);
                        }
                    }
                });

                holder.itemView.setOnClickListener(view -> {
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });
                break;
            }
            case 32231: {
                ChatHolder_File_ReplyTrue_Other_Group holder = (ChatHolder_File_ReplyTrue_Other_Group) viewHolder;
                holder.tvMessage.setText(String.format("%s", chatMessage.getMessage().substring(chatMessage.getMessage().indexOf("_nznv_") + 6)));
                if (chatMessage.getProgress() != null) {
                    if (chatMessage.getProgress() == -1 || chatMessage.getProgress() == 100) {
                        holder.tvProgress.setVisibility(View.GONE);
                    } else {
                        holder.tvProgress.setVisibility(View.VISIBLE);
                        holder.tvProgress.setText(String.format("در حال دانلود... %s%s کامل شده", chatMessage.getProgress(), "%"));
                    }
                } else {
                    holder.tvProgress.setVisibility(View.GONE);
                }
                switch (chatMessage.getReply().getMessageType()) {
                    case "text":
                        holder.tvReply.setVisibility(View.VISIBLE);
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        break;
                    case "picture":
                        holder.cardViewReplyPicture.setVisibility(View.VISIBLE);
                        holder.tvReply.setVisibility(View.GONE);
                        Glide.with(context).load(FILE_URL + chatMessage.getReply().getMessage()).into(holder.imageViewReplyMessage);
                        break;
                    case "file":
                        holder.cardViewReplyPicture.setVisibility(View.GONE);
                        holder.tvReply.setVisibility(View.VISIBLE);
                        break;
                }
                if (chatMessage.getReply().getMessageType().equals("file")) {
                    if (chatMessage.getReply().getMessage().contains("_nznv_")) {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage().substring(chatMessage.getReply().getMessage().indexOf("_nznv_") + 6)));
                    } else {
                        holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                    }
                } else {
                    holder.tvReply.setText(String.format("%s", chatMessage.getReply().getMessage()));
                }
                if (chatMessage.getUpdatedAt() != null) {
                    String normalizedDate = chatMessage.getUpdatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                } else {
                    String normalizedDate = chatMessage.getCreatedAt().replace(".000Z", "").replace("T", " ");
                    DateUtils.DateObject dateObject = dateHelper.getParsedDate(normalizedDate);
                    holder.tvTime.setText(getTime(dateObject));
                }

                holder.linearLayoutReply.setOnClickListener(view -> getMessagePosition(chatMessage.getReplyMessageId(), SearchType.REPLY));

                if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                    holder.coordinatorDownloadedFile.setVisibility(View.GONE);
                } else {
                    holder.coordinatorDownloadedFile.setVisibility(View.VISIBLE);
                }
                holder.coordinatorDownloadFile.setOnClickListener(v -> {
                    if (checkReadExternalPermission(activity)) {
                        if (FileDownloaderNew.isFileExists(context, chatMessage.getMessage())) {
                            FileDownloaderNew.openFile(context, chatMessage.getMessage());
                        } else {
                            downloadFile(chatMessage);
//                            FileDownloader fileDownloader = new FileDownloader();
//                            fileDownloader.setOnFileDownloadListener(() -> {
//                                FileDownloaderNew.openFile(context, chatMessage.getMessage());
//                                notifyItemChanged(position);
//                            });
//                            fileDownloader.execute(chatMessage.getMessage(), context, FILE_URL);
                        }
                    }
                });

                holder.itemView.setOnClickListener(view -> {
                    PopupMenu popup = new PopupMenu(activity, (holder.tvTime));
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.popup_menu_chat_click_message_other, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.reply) {
                            iChatUtils.onReply(chatMessage);
                        }
                        return false;
                    });
                });
                String name = "";
                if (chatMessage.getUser().getFirstName() != null) {
                    name = chatMessage.getUser().getFirstName();
                }
                if (chatMessage.getUser().getLastName() != null) {
                    name += " " + chatMessage.getUser().getLastName();
                }
                holder.tvUserName.setText(String.format("%s", name));
                Glide.with(context).load(FILE_URL + chatMessage.getUser().getPicture()).placeholder(ContextCompat.getDrawable(context, R.drawable.ic_user_circle)).into(holder.imageViewProfile);
                break;
            }
        }
    }

    public int getItemCount() {
        return this.chatMessages.size();
    }

    public void addItem(Message message) {
        this.chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
        this.recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    public void deleteItem(Integer messageId) {
        int i = 0;
        for (Message message : chatMessages) {
            if (message.getId().equals(messageId)) {
                chatMessages.remove(i);
                notifyItemRemoved(i);
                break;
            }
            i++;
        }
    }

    public void updateItem(Message message) {
        int i = 0;
        for (Message item : chatMessages) {
            if (item.getId().equals(message.getId())) {
                item.setMessage(message.getMessage());
                notifyItemChanged(i);
                break;
            }
            i++;
        }
    }

    public void updateFileItemProgress(int id, int progress) {
        int i = 0;
        for (Message item : chatMessages) {
            if (item.getId().equals(id)) {
                item.setProgress(progress);
                notifyItemChanged(i);
                break;
            }
            i++;
        }
    }


    public void seenItem(Integer messageId) {
        int i = 0;
        for (Message message : chatMessages) {
            if (message.getId().equals(messageId)) {
                message.setSeenCount(message.getSeenCount() + 1);
                notifyItemChanged(i);
                break;
            }
            i++;
        }
    }

    public String getTime(DateUtils.DateObject dateObject) {
        if (TildaChatApp._isTime48) {
            return DateUtils.getTime48WithZero(dateObject.hour, dateObject.minute);
        } else {
            return DateUtils.getTimeWithZero(dateObject.hour, dateObject.minute);
        }
    }

    public void addItems(int page, List<Message> input) {
        for (Message chatMessage : input) {
            this.chatMessages.add(0, chatMessage);
            notifyItemInserted(0);
        }
        if (page == 1) {
            this.recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        }
    }

    public void setRoomType(ChatroomType roomType) {
        this.roomType = roomType;
    }

    public void setRoomAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public void setLoaded() {
        this.loading = false;
    }

    //Search
    public void getMessagePosition(int messageId, SearchType searchType) {
        for (int i = 0; i < chatMessages.size(); i++) {
            if (chatMessages.get(i).getId() == messageId) {
                iChatUtils.onLoadMoreForSearchFinish();
                if (searchType == SearchType.REPLY) {
                    showReplySearch(i);
                }
                return;
            }
        }
        iChatUtils.onLoadMoreForSearch(messageId, searchType);
    }

    private void showReplySearch(int position) {
        if (position >= 0) {
            recyclerView.scrollToPosition(position);
//            chatMessages.get(position).setReplyWave(true);
            notifyItemChanged(position);
        }
    }

    protected boolean checkReadExternalPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            } else {
                return true;
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            } else {
                return true;
            }
        }
    }

    public void clearAll() {
        this.chatMessages.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onFileDownloaded(String path) {

    }

    @Override
    public void onFileDownloaded(Long downloadId) {

    }

    private void downloadFile(Message chatMessage) {
        if (isDownloadingFile) {

        } else {
            isDownloadingFile = true;
            android.os.Message message = android.os.Message.obtain();
            message.what = UPDATE_DOWNLOAD_PROGRESS;
            message.arg1 = 0;
            message.arg2 = chatMessage.getId();
            mainHandler.sendMessage(message);
            FileDownloaderNew fileDownloaderNew = new FileDownloaderNew(context, TildaChatApp._downloadFolder);
            fileDownloaderNew.setOnFileDownloadListener(new FileDownloaderNew.OnFileDownloadListener() {
                @Override
                public void onFileDownloaded(String path) {

                }

                @Override
                public void onFileDownloaded(Long downloadId) {

                    // Run a task in a background thread to check download progress
                    if (executor == null || executor.isShutdown()) {
                        executor = Executors.newFixedThreadPool(1);
                        executor.execute(() -> {
                            int progress = 0;
                            boolean isDownloadFinished = false;
                            while (!isDownloadFinished) {
                                try {
                                    Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                                    if (cursor != null && cursor.moveToFirst()) {
                                        @SuppressLint("Range") int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                        switch (downloadStatus) {
                                            case DownloadManager.STATUS_RUNNING:
                                                @SuppressLint("Range") long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                                if (totalBytes > 0) {
                                                    @SuppressLint("Range") long downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                                    progress = (int) (downloadedBytes * 100 / totalBytes);
                                                }

                                                break;
                                            case DownloadManager.STATUS_SUCCESSFUL:
                                                progress = 100;
                                                isDownloadFinished = true;
                                                isDownloadingFile = false;
                                                break;
                                            case DownloadManager.STATUS_PAUSED:
                                            case DownloadManager.STATUS_PENDING:
                                                break;
                                            case DownloadManager.STATUS_FAILED:
                                                progress = 0;
                                                isDownloadFinished = true;
                                                isDownloadingFile = false;
                                                break;
                                        }
                                        cursor.close();
                                        android.os.Message message = android.os.Message.obtain();
                                        message.what = UPDATE_DOWNLOAD_PROGRESS;
                                        message.arg1 = progress;
                                        message.arg2 = chatMessage.getId();
                                        mainHandler.sendMessage(message);
                                    } else {
                                        isDownloadingFile = false;
                                        isDownloadFinished = true;
                                        progress = 0;
                                        android.os.Message message = android.os.Message.obtain();
                                        message.what = UPDATE_DOWNLOAD_PROGRESS;
                                        message.arg1 = progress;
                                        message.arg2 = chatMessage.getId();
                                        mainHandler.sendMessage(message);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
            fileDownloaderNew.execute(FILE_URL + chatMessage.getMessage());
        }
    }

    // Indicate that we would like to update download progress
    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
    // Use a background thread to check the progress of downloading
    private ExecutorService executor;
    // Use a handler to update progress bar on the main thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull android.os.Message msg) {
            if (msg.what == UPDATE_DOWNLOAD_PROGRESS) {
                int downloadProgress = msg.arg1;
                if (downloadProgress % 4 == 0) {
                    updateFileItemProgress(msg.arg2, downloadProgress);
                }
                if (downloadProgress == 100 || downloadProgress == -1) {
                    updateFileItemProgress(msg.arg2, 100);
                    executor.shutdown();
                    mainHandler.removeCallbacksAndMessages(null);
                }
                // Update your progress bar here.

//                progressBar.setProgress(downloadProgress);
            }
            return true;
        }
    });
}
