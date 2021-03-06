package ir.tildaweb.tildachat.ui.chatroom_messaging;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aghajari.emojiview.view.AXEmojiPopup;
import com.aghajari.emojiview.view.AXEmojiView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ir.tildaweb.tilda_filepicker.TildaFilePicker;
import ir.tildaweb.tilda_filepicker.enums.FileType;
import ir.tildaweb.tilda_filepicker.models.FileModel;
import ir.tildaweb.tildachat.R;
import ir.tildaweb.tildachat.adapter.AdapterPrivateChatMessages;
import ir.tildaweb.tildachat.app.DataParser;
import ir.tildaweb.tildachat.app.SocketEndpoints;
import ir.tildaweb.tildachat.app.TildaChatApp;
import ir.tildaweb.tildachat.databinding.ActivityChatroomMessagingBinding;
import ir.tildaweb.tildachat.dialogs.DialogConfirmMessage;
import ir.tildaweb.tildachat.enums.ChatroomType;
import ir.tildaweb.tildachat.enums.MessageType;
import ir.tildaweb.tildachat.interfaces.IChatUtils;
import ir.tildaweb.tildachat.interfaces.LoadMoreData;
import ir.tildaweb.tildachat.models.base_models.Message;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitChatroomCheck;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitChatroomJoin;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitChatroomMessages;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitMessageDelete;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitMessageSeen;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitMessageStore;
import ir.tildaweb.tildachat.models.connection_models.emits.EmitMessageUpdate;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveChatroomCheck;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveChatroomJoin;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveChatroomMessages;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveMessageDelete;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveMessageSeen;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveMessageStore;
import ir.tildaweb.tildachat.models.connection_models.receives.ReceiveMessageUpdate;
import ir.tildaweb.tildachat.services.TildaFileUploaderForegroundService;
import ir.tildaweb.tildachat.utils.MathUtils;

public class ChatroomMessagingActivity extends AppCompatActivity implements View.OnClickListener, LoadMoreData, IChatUtils {

    private String TAG = this.getClass().getName();
    private ActivityChatroomMessagingBinding activityChatroomMessagingBinding;
    private Integer userId;
    private String roomId;
    private static String FILE_URL;
    private static String UPLOAD_ROUTE;
    private AXEmojiPopup emojiPopup;

    private String roomTitle;
    private String roomPicture;
    private Integer secondUserId = null;
    private String roomType;
    private Integer chatroomId;
    private int nextPage = 0;
    private int lastPage = 0;
    private AdapterPrivateChatMessages adapterPrivateChatMessages;
    private Integer replyMessageId = null;
    private boolean isUpdate = false;
    private Integer updateMessageId = null;
    private boolean isSearchForReply = false;
    private Integer searchMessageId;

    //Image File
    private int PICK_IMAGE_PERMISSION_CODE = 1001;
    private int PICK_FILE_PERMISSION_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("is_status_bar_light", false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        activityChatroomMessagingBinding = ActivityChatroomMessagingBinding.inflate(getLayoutInflater());
        setContentView(activityChatroomMessagingBinding.getRoot());

        AXEmojiView emojiView = new AXEmojiView(ChatroomMessagingActivity.this);
        emojiView.setEditText(activityChatroomMessagingBinding.etMessage);
        emojiPopup = new AXEmojiPopup(emojiView);
        //Get intent info
        userId = getIntent().getIntExtra("user_id", -1);
        FILE_URL = getIntent().getStringExtra("file_url");
        UPLOAD_ROUTE = getIntent().getStringExtra("upload_route");
        if (getIntent().hasExtra("room_id"))
            roomId = getIntent().getExtras().getString("room_id");

        activityChatroomMessagingBinding.etMessage.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewEmoji.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewBack.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewReplyClose.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewUpdateClose.setOnClickListener(this);
        activityChatroomMessagingBinding.linearChatroomDetails.setOnClickListener(this);
        activityChatroomMessagingBinding.tvJoinChannel.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewSend.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewImage.setOnClickListener(this);
        activityChatroomMessagingBinding.imageViewFile.setOnClickListener(this);

        activityChatroomMessagingBinding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        adapterPrivateChatMessages = new AdapterPrivateChatMessages(this, ChatroomMessagingActivity.this, userId, FILE_URL, activityChatroomMessagingBinding.recyclerViewMessages, new ArrayList<>(), this, this);
        activityChatroomMessagingBinding.recyclerViewMessages.setAdapter(adapterPrivateChatMessages);

        activityChatroomMessagingBinding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    activityChatroomMessagingBinding.imageViewFile.setVisibility(View.GONE);
                    activityChatroomMessagingBinding.imageViewVoice.setVisibility(View.GONE);
                    activityChatroomMessagingBinding.imageViewImage.setVisibility(View.GONE);
                    activityChatroomMessagingBinding.imageViewSend.setVisibility(View.VISIBLE);
                } else {
                    activityChatroomMessagingBinding.imageViewFile.setVisibility(View.VISIBLE);
                    activityChatroomMessagingBinding.imageViewVoice.setVisibility(View.VISIBLE);
                    activityChatroomMessagingBinding.imageViewImage.setVisibility(View.VISIBLE);
                    activityChatroomMessagingBinding.imageViewSend.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        setSocketListeners();
        EmitChatroomCheck emitChatroomCheck = new EmitChatroomCheck();
        emitChatroomCheck.setRoomId(roomId);
        emitChatroomCheck.setType(EmitChatroomCheck.ChatroomCheckType.ROOM_ID);
        TildaChatApp.getSocketRequestController().emitter().emitChatroomCheck(emitChatroomCheck);
    }

    private void setChatroomInfo() {
        activityChatroomMessagingBinding.tvUserName.setText(String.valueOf(roomTitle));
        if (roomPicture != null) {
            Glide.with(this).load(FILE_URL + roomPicture).into(activityChatroomMessagingBinding.imageViewProfilePicture);
        }
        if (roomType.equals("channel")) {
            activityChatroomMessagingBinding.linearChatBox.setVisibility(View.GONE);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            activityChatroomMessagingBinding.recyclerViewMessages.setLayoutParams(layoutParams);
        }
    }

    private void join() {
        EmitChatroomJoin emitChatroomJoin = new EmitChatroomJoin();
        emitChatroomJoin.setRoomId(roomId);
        TildaChatApp.getSocketRequestController().emitter().emitChatroomJoin(emitChatroomJoin);
    }

    private void setSocketListeners() {
        TildaChatApp.getSocketRequestController().receiver().receiveChatroomCheck(ChatroomMessagingActivity.this, ReceiveChatroomCheck.class, response -> {
            Log.d(TAG, "setSocketListeners: CheckChatroom");
            if (response.getChatroom() != null) {
                if (response.getChatroom().getType().equals("channel")) {
                    adapterPrivateChatMessages.setRoomType(ChatroomType.CHANNEL);
                } else if (response.getChatroom().getType().equals("group")) {
                    adapterPrivateChatMessages.setRoomType(ChatroomType.GROUP);
                }
            }
            switch (response.getCode()) {
                case 111:
                case 112: {
                    // Channel info, user is joined to channel if code=111
//                    roomTitle = response.getChatroom().getRoomTitle();
//                    roomPicture = response.getChatroom().getRoomPicture();
//                    roomType = response.getChatroom().getType();
//                    chatroomId = response.getChatroom().getId();
//                    roomId = response.getChatroom().getRoomId();
//                    boolean isJoin = response.getJoin();
//                    if (isJoin) {
//                        activityChatroomMessagingBinding.linearJoinChannel.setVisibility(View.GONE);
//                    } else {
//                        activityChatroomMessagingBinding.linearJoinChannel.setVisibility(View.VISIBLE);
//                    }
//                    activityChatroomMessagingBinding.tvUserStatus.setText(MathUtils.convertNumberToKilo(response.getMemberCount()) + " ??????");
//                    setChatroomInfo();
//                    join();
                    break;
                }
                case 121: {
                    //Private chat info , users have chatroom
                    if (response.getSecondUser().getFirstName() != null) {
                        roomTitle = response.getSecondUser().getFirstName();
                    }
                    if (response.getSecondUser().getLastName() != null) {
                        roomTitle += " " + response.getSecondUser().getLastName();
                    }
                    roomPicture = response.getSecondUser().getPicture();
                    roomType = "private";
                    chatroomId = response.getChatroom().getId();
                    roomId = response.getChatroom().getRoomId();
                    activityChatroomMessagingBinding.linearChatBox.setVisibility(View.VISIBLE);
                    setChatroomInfo();
                    join();
                    break;
                }
                case 122: {
                    //Private chat info , users don't have chatroom
                    if (response.getSecondUser().getFirstName() != null) {
                        roomTitle = response.getSecondUser().getFirstName();
                    }
                    if (response.getSecondUser().getLastName() != null) {
                        roomTitle += " " + response.getSecondUser().getLastName();
                    }
                    roomPicture = response.getSecondUser().getPicture();
                    secondUserId = response.getSecondUser().getId();
                    roomType = "private";
                    chatroomId = null;
                    roomId = null;
                    activityChatroomMessagingBinding.linearChatBox.setVisibility(View.VISIBLE);
                    setChatroomInfo();
                    break;
                }
                case 123: {
                    Toast.makeText(this, "???????????? ???? ?????? ???? ???????? ??????????.", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
                case 131: {
                    // Group info, user is joined to group
                    roomTitle = response.getChatroom().getRoomTitle();
                    roomPicture = response.getChatroom().getRoomPicture();
                    roomType = response.getChatroom().getType();
                    chatroomId = response.getChatroom().getId();
                    roomId = response.getChatroom().getRoomId();
                    activityChatroomMessagingBinding.linearChatBox.setVisibility(View.VISIBLE);
                    activityChatroomMessagingBinding.tvUserStatus.setText(String.format("%s %s", MathUtils.convertNumberToKilo(response.getMemberCount()), "??????"));
                    setChatroomInfo();
                    join();
                    break;
                }
                case 132: {
                    toast("???????????? ?????? ???? ?????? ???? ?????????? ???????? ????????.");
                    finish();
                    break;
                }
            }
        });

        TildaChatApp.getSocketRequestController().receiver().receiveChatroomJoin(this, ReceiveChatroomJoin.class, response -> {
            nextPage = 0;
            EmitChatroomMessages emitChatroomMessages = new EmitChatroomMessages();
            emitChatroomMessages.setRoomId(roomId);
            emitChatroomMessages.setUserId(1);
            emitChatroomMessages.setPage(++nextPage);
            adapterPrivateChatMessages.clearAll();
            TildaChatApp.getSocketRequestController().emitter().emitChatroomMessages(emitChatroomMessages);
        });

        TildaChatApp.getSocketRequestController().receiver().receiveChatroomMessages(this, ReceiveChatroomMessages.class, response -> {
            if (response.getRoomId().equals(roomId)) {
                if (response.getStatus() == 200) {
                    activityChatroomMessagingBinding.noItem.setVisibility(View.GONE);
                    if (nextPage > lastPage) {
                        lastPage = nextPage;
                        adapterPrivateChatMessages.addItems(nextPage, response.getMessages());
                        adapterPrivateChatMessages.setLoaded();
                        if (isSearchForReply) {
                            adapterPrivateChatMessages.getMessagePosition(searchMessageId, AdapterPrivateChatMessages.SearchType.REPLY);
                        }
                    }
                } else if (response.getStatus() == 404 && adapterPrivateChatMessages.getItemCount() == 0) {
                    activityChatroomMessagingBinding.noItem.setVisibility(View.VISIBLE);
                }
            }
        });

        TildaChatApp.getSocketRequestController().receiver().receiveMessageStore(this, ReceiveMessageStore.class, response -> {
            Log.d(TAG, "setSocketListeners:Store.............:  " + DataParser.toJson(response));
            if (roomId.equals(response.getRoomId())) {
                if (response.getStatus() == 200) {
                    adapterPrivateChatMessages.addItem(response.getMessage());
                    if (activityChatroomMessagingBinding.noItem.getVisibility() == View.VISIBLE) {
                        activityChatroomMessagingBinding.noItem.setVisibility(View.GONE);
                    }
                }
            }
        });

        TildaChatApp.getSocketRequestController().receiver().receiveMessageSeen(this, ReceiveMessageSeen.class, response -> {
            if (response.getRoomId().equals(roomId)) {
                if (response.getStatus() == 200) {
                    adapterPrivateChatMessages.seenItem(response.getMessageId());
                }
            }
        });

        TildaChatApp.getSocketRequestController().receiver().receiveMessageUpdate(this, ReceiveMessageUpdate.class, response -> {
            if (response.getRoomId().equals(roomId)) {
                if (response.getStatus() == 200) {
                    adapterPrivateChatMessages.updateItem(response.getMessage());
                }
            }
        });

        TildaChatApp.getSocketRequestController().receiver().receiveMessageDelete(this, ReceiveMessageDelete.class, response -> {
            Log.d(TAG, "setSocketListeners: " + DataParser.toJson(response));
            if (response.getRoomId().equals(roomId)) {
                if (response.getStatus() == 200) {
                    adapterPrivateChatMessages.deleteItem(response.getMessageId());
                }
            }
        });

//        socket.on(SocketEndpoints.TAG_CLIENT_RECEIVE_ERROR, args -> runOnUiThread(() -> toast("?????????? ???? ??????.")));
//        socket.on(SocketEndpoints.TAG_CLIENT_RECEIVE_CHATROOM_CHANNEL_MEMBERSHIP, args -> runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                String room = (String) args[0];
//                int status = (int) args[1];
//                String message = (String) args[2];
//                if (room.equals(roomId)) {
//                    if (status == 200) {
//                        //Joined to channel (Subscription)
//                        activityChatroomMessagingBinding.linearJoinChannel.setVisibility(View.GONE);
//                        toast("?????? ?????? ?????????? ????????.");
//                    } else {
//                        toast("?????????? ?????????? ???? ?????????? ???????? ??????????. ?????????? ???????????? ????????.");
//                    }
//                }
//            }
//        }));
    }

    @Override
    public void onBackPressed() {
        TildaChatApp.getSocket().off(SocketEndpoints.TAG_RECEIVE_CHATROOM_CHECK);
        TildaChatApp.getSocket().off(SocketEndpoints.TAG_RECEIVE_CHATROOM_JOIN);
        TildaChatApp.getSocket().off(SocketEndpoints.TAG_RECEIVE_CHATROOM_MESSAGES);
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == RESULT_OK) {
//            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//                CropImage.ActivityResult result = CropImage.getActivityResult(data);
//                Uri resultUri = result.getUri();
//                if (resultUri != null && resultUri.getPath() != null) {
//                    Intent intent = new Intent(ChatroomMessagingActivity.this, TildaFileUploaderForegroundService.class);
//                    intent.setAction("chat_uploader");
//                    intent.putExtra("file_path", resultUri.getPath());
//                    intent.putExtra("upload_route", UPLOAD_ROUTE);
//                    intent.putExtra("is_send_to_chatroom", true);
//                    intent.putExtra("chatroom_id", chatroomId);
//                    intent.putExtra("file_type", MessageType.PICTURE);
//                    intent.putExtra("room_id", roomId);
//                    intent.putExtra("second_user_id", secondUserId);
//                    intent.putExtra("is_second_user", roomId == null);
//                    startService(intent);
////                        EmitMessageNew emitMessageNew = new EmitMessageNew();
////                        emitMessageNew.setType("picture");
////                        emitMessageNew.setMessage(fileBase64);
////                        emitMessageNew.setReplyMessageId(replyMessageId);
////                        emitMessageNew.setReply(isReply);
////                        emitMessageNew.setUpdate(isUpdate);
////                        if (roomId == null) {
////                            emitMessageNew.setSecondUserId(secondUserId);
////                        }
////                        Log.d(TAG, "onClick: " + DataParser.toJson(emitMessageNew));
////                        socket.emit(SocketEndpoints.TAG_CLIENT_SEND_MESSAGE_NEW, roomId, roomId, DataParser.toJson(emitMessageNew));
////
////                        //Reset state
////                        activityChatroomMessagingBinding.etMessage.setText("");
////                        resetReply();
//
//                } else {
//                    toast("???????????? ?????????? ???? ???????? ?????????? ????. ???????? ?????????? ?????????? ???? ???????????? ????????.");
//                }
//            }
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_IMAGE_PERMISSION_CODE) {
            activityChatroomMessagingBinding.imageViewImage.callOnClick();
        }
        if (requestCode == PICK_FILE_PERMISSION_CODE) {
            activityChatroomMessagingBinding.imageViewFile.callOnClick();
        }
    }

    private void resetReply() {
        activityChatroomMessagingBinding.tvReplyMessage.setText("");
        activityChatroomMessagingBinding.linearReply.setVisibility(View.GONE);
        replyMessageId = null;
    }

    private void resetUpdate() {
        activityChatroomMessagingBinding.tvUpdateMessage.setText("");
        activityChatroomMessagingBinding.etMessage.setText("");
        activityChatroomMessagingBinding.linearUpdate.setVisibility(View.GONE);
        isUpdate = false;
        updateMessageId = null;
    }

    @Override
    public void onLoadMore() {
        EmitChatroomMessages emitChatroomMessages = new EmitChatroomMessages();
        emitChatroomMessages.setRoomId(roomId);
        emitChatroomMessages.setPage(++nextPage);
        TildaChatApp.getSocketRequestController().emitter().emitChatroomMessages(emitChatroomMessages);
    }

    @Override
    public void onCopy() {
        Toast.makeText(this, "?????? ????.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReply(Message message) {
        if (message.getMessageType().equals("file")) {
            activityChatroomMessagingBinding.tvReplyMessage.setText(String.format("%s: %s", "????????", message.getMessage().substring(message.getMessage().indexOf("_nznv_") + 6)));
        } else {
            activityChatroomMessagingBinding.tvReplyMessage.setText(String.format("%s", message.getMessage()));
        }
        activityChatroomMessagingBinding.linearReply.setVisibility(View.VISIBLE);
        replyMessageId = message.getId();
    }

    @Override
    public void onEdit(Message message) {
        activityChatroomMessagingBinding.etMessage.setText(String.format("%s", message.getMessage()));
        activityChatroomMessagingBinding.tvUpdateMessage.setText(String.format("%s", message.getMessage()));
        activityChatroomMessagingBinding.linearUpdate.setVisibility(View.VISIBLE);
        isUpdate = true;
        updateMessageId = message.getId();
    }

    @Override
    public void onDelete(Message message) {
        String description = "?????? ???? ???????????? ?????? ???????? ?????? ????????";
        DialogConfirmMessage dialogConfirmMessage = new DialogConfirmMessage(ChatroomMessagingActivity.this, "?????? ????????", description);
        dialogConfirmMessage.setOnCancelClickListener(view -> dialogConfirmMessage.dismiss());
        dialogConfirmMessage.setOnConfirmClickListener(view -> {
            EmitMessageDelete emitMessageDelete = new EmitMessageDelete();
            emitMessageDelete.setMessageId(message.getId());
            emitMessageDelete.setRoomId(roomId);
            TildaChatApp.getSocketRequestController().emitter().emitMessageDelete(emitMessageDelete);
            dialogConfirmMessage.dismiss();
        });

        dialogConfirmMessage.show();
    }

    @Override
    public void onLoadMoreForSearch(int messageId, AdapterPrivateChatMessages.
            SearchType searchType) {
//        showLoadingFullPage();
        isSearchForReply = searchType == AdapterPrivateChatMessages.SearchType.REPLY;
        searchMessageId = messageId;
        onLoadMore();
    }

    @Override
    public void onLoadMoreForSearchFinish() {
//        dismissLoading();
    }

    @Override
    public void onMessageSeen(int messageId) {
        EmitMessageSeen emitMessageSeen = new EmitMessageSeen();
        emitMessageSeen.setMessageId(messageId);
        emitMessageSeen.setUserId(userId);
        emitMessageSeen.setRoomId(roomId);
        TildaChatApp.getSocketRequestController().emitter().emitMessageSeen(emitMessageSeen);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imageViewBack) {
            onBackPressed();
        } else if (id == R.id.imageViewSend) {
            String message = activityChatroomMessagingBinding.etMessage.getText().toString();
            if (message.length() > 0) {
                if (isUpdate) {
                    EmitMessageUpdate emitMessageUpdate = new EmitMessageUpdate();
                    emitMessageUpdate.setMessage(message);
                    emitMessageUpdate.setUpdate(isUpdate);
                    emitMessageUpdate.setRoomId(roomId);
                    emitMessageUpdate.setMessageId(updateMessageId);
                    TildaChatApp.getSocketRequestController().emitter().emitMessageUpdate(emitMessageUpdate);
                    //Reset state
                    resetUpdate();
                } else {
                    EmitMessageStore emitMessageStore = new EmitMessageStore();
                    emitMessageStore.setType(MessageType.TEXT);
                    emitMessageStore.setMessage(message);
                    emitMessageStore.setReplyMessageId(replyMessageId);
                    emitMessageStore.setUpdate(isUpdate);
                    emitMessageStore.setChatroomId(chatroomId);
                    if (roomId != null) {
                        emitMessageStore.setRoomId(roomId);
                    } else {
                        emitMessageStore.setSecondUserId(secondUserId);
                    }
                    Log.d(TAG, "onClick: " + DataParser.toJson(emitMessageStore));
                    TildaChatApp.getSocketRequestController().emitter().emitMessageStore(emitMessageStore);
                    //Reset state
                    activityChatroomMessagingBinding.etMessage.setText("");
                    resetReply();
                }
            }
        } else if (id == R.id.imageViewReplyClose) {
            resetReply();
        } else if (id == R.id.imageViewUpdateClose) {
            resetUpdate();
        } else if (id == R.id.tvJoinChannel) {
            //Todo: channel abilities not yet.
//            socket.emit(SocketEndpoints.TAG_CLIENT_SEND_CHATROOM_CHANNEL_MEMBERSHIP, roomId, chatroomId, userId);
        } else if (id == R.id.imageViewImage) {
            TildaFilePicker tildaFilePicker = new TildaFilePicker(ChatroomMessagingActivity.this, new FileType[]{FileType.FILE_TYPE_IMAGE});
            tildaFilePicker.setOnTildaFileSelectListener(list -> {
                for (FileModel model : list) {
                    Intent intent = new Intent(ChatroomMessagingActivity.this, TildaFileUploaderForegroundService.class);
                    intent.setAction("chat_uploader");
                    intent.putExtra("file_path", model.getPath());
                    intent.putExtra("upload_route", UPLOAD_ROUTE);
                    intent.putExtra("is_send_to_chatroom", true);
                    intent.putExtra("chatroom_id", chatroomId);
                    intent.putExtra("message_type", MessageType.PICTURE.label);
                    intent.putExtra("room_id", roomId);
                    intent.putExtra("second_user_id", secondUserId);
                    intent.putExtra("is_second_user", roomId == null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }
            });
            tildaFilePicker.show(getSupportFragmentManager());
        } else if (id == R.id.imageViewFile) {
            TildaFilePicker tildaFilePicker = new TildaFilePicker(ChatroomMessagingActivity.this);
            tildaFilePicker.setOnTildaFileSelectListener(list -> {
                for (FileModel model : list) {
                    Intent intent = new Intent(ChatroomMessagingActivity.this, TildaFileUploaderForegroundService.class);
                    intent.setAction("chat_uploader");
                    intent.putExtra("file_path", model.getPath());
                    intent.putExtra("upload_route", UPLOAD_ROUTE);
                    intent.putExtra("is_send_to_chatroom", true);
                    intent.putExtra("chatroom_id", chatroomId);
                    intent.putExtra("message_type", MessageType.FILE.label);
                    intent.putExtra("room_id", roomId);
                    intent.putExtra("second_user_id", secondUserId);
                    intent.putExtra("is_second_user", roomId == null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }
            });
            tildaFilePicker.show(getSupportFragmentManager());
        } else if (id == R.id.linearChatroomDetails) {
//            Intent intent = new Intent(ChatroomMessagingActivity.this, ChatroomDetailsActivity.class);
//            intent.putExtra("room_id", chatroomId);
//            intent.putExtra("room_name", roomId);
//            intent.putExtra("room_type", roomType);
//            startActivity(intent);
        } else if (id == R.id.imageViewEmoji) {
            if (emojiPopup.isShowing()) {
                activityChatroomMessagingBinding.imageViewEmoji.setImageDrawable(ContextCompat.getDrawable(ChatroomMessagingActivity.this, R.drawable.ic_smile));
            } else {
                activityChatroomMessagingBinding.imageViewEmoji.setImageDrawable(ContextCompat.getDrawable(ChatroomMessagingActivity.this, R.drawable.ic_type));
            }
            emojiPopup.toggle();
        } else if (id == R.id.etMessage) {
            if (emojiPopup.isShowing()) {
                activityChatroomMessagingBinding.imageViewEmoji.setImageDrawable(ContextCompat.getDrawable(ChatroomMessagingActivity.this, R.drawable.ic_smile));
                emojiPopup.dismiss();
            }
        }
    }

    private void requestFilePermission() {
        ActivityResultLauncher<String[]> filePermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean readPermission = result.get(
                                    Manifest.permission.READ_EXTERNAL_STORAGE);
                            Boolean writePermission = result.get(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
                            if (readPermission != null && writePermission != null) {

                            } else {
                                toast("?????? ?????????? ???????????? ???? ???????? ???? ????????????.");
                            }
                        }
                );
        filePermissionRequest.launch(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }

    private void toast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}