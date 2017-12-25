package io.github.izzyleung.zhihudailypurify.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.github.izzyleung.ZhihuDailyPurify;
import io.github.izzyleung.zhihudailypurify.R;
import io.github.izzyleung.zhihudailypurify.ZhihuDailyPurifyApplication;
import io.github.izzyleung.zhihudailypurify.support.Check;
import io.github.izzyleung.zhihudailypurify.support.Constants;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.CardViewHolder> {
    private ZhihuDailyPurify.Feed feed;

    private ImageLoader imageLoader = ImageLoader.getInstance();
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.noimage)
            .showImageOnFail(R.drawable.noimage)
            .showImageForEmptyUri(R.drawable.lks_for_blank_url)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();
    private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

    public NewsAdapter(ZhihuDailyPurify.Feed feed) {
        this.feed = feed;

        setHasStableIds(true);
    }

    public void updateFeed(ZhihuDailyPurify.Feed feed) {
        this.feed = feed;
        notifyDataSetChanged();
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        View itemView = LayoutInflater
                .from(context)
                .inflate(R.layout.news_list_item, parent, false);

        return new CardViewHolder(itemView, new CardViewHolder.ClickResponseListener() {
            @Override
            public void onWholeClick(int position) {
                browse(context, position);
            }

            @Override
            public void onOverflowClick(View v, int position) {
                PopupMenu popup = new PopupMenu(context, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.contextual_news_list, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {

                    if (item.getItemId() == R.id.action_share_url) {
                        share(context, position);
                    }
                    
                    return true;
                });
                popup.show();
            }
        });
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        ZhihuDailyPurify.News news = feed.getNewsList().get(position);
        imageLoader.displayImage(news.getThumbnailUrl(), holder.newsImage, options, animateFirstListener);

        if (news.getQuestionsList().size() > 1) {
            holder.questionTitle.setText(news.getTitle());
            holder.dailyTitle.setText(Constants.Strings.MULTIPLE_DISCUSSION);
        } else {
            holder.questionTitle.setText(news.getQuestionsList().get(0).getTitle());
            holder.dailyTitle.setText(news.getTitle());
        }
    }

    @Override
    public int getItemCount() {
        return feed.getNewsList().size();
    }

    @Override
    public long getItemId(int position) {
        return feed.getNewsList().get(position).hashCode();
    }

    private void browse(Context context, int position) {
        ZhihuDailyPurify.News news = feed.getNewsList().get(position);

        if (news.getQuestionsList().size() > 1) {
            AlertDialog dialog = createDialog(context,
                    news,
                    makeGoToZhihuDialogClickListener(context, news));
            dialog.show();
        } else {
            goToZhihu(context, news.getQuestionsList().get(0).getUrl());
        }
    }

    private void share(Context context, int position) {
        ZhihuDailyPurify.News news = feed.getNewsList().get(position);

        if (news.getQuestionsList().size() > 1) {
            AlertDialog dialog = createDialog(context,
                    news,
                    makeShareQuestionDialogClickListener(context, news));
            dialog.show();
        } else {
            shareQuestion(context,
                    news.getQuestionsList().get(0).getTitle(),
                    news.getQuestionsList().get(0).getUrl());
        }
    }

    private AlertDialog createDialog(Context context, ZhihuDailyPurify.News news, DialogInterface.OnClickListener listener) {
        String[] questionTitles = getQuestionTitlesAsStringArray(news);

        return new AlertDialog.Builder(context)
                .setTitle(news.getTitle())
                .setItems(questionTitles, listener)
                .create();
    }

    private DialogInterface.OnClickListener makeGoToZhihuDialogClickListener(Context context, ZhihuDailyPurify.News dailyNews) {
        return (dialog, which) -> {
            String questionUrl = dailyNews.getQuestionsList().get(which).getUrl();

            goToZhihu(context, questionUrl);
        };
    }

    private DialogInterface.OnClickListener makeShareQuestionDialogClickListener(Context context, ZhihuDailyPurify.News dailyNews) {
        return (dialog, which) -> {
            String questionTitle = dailyNews.getQuestionsList().get(which).getTitle(),
                    questionUrl = dailyNews.getQuestionsList().get(which).getUrl();

            shareQuestion(context, questionTitle, questionUrl);
        };
    }

    private void goToZhihu(Context context, String url) {
        if (!ZhihuDailyPurifyApplication.getSharedPreferences()
                .getBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_USE_CLIENT, false)) {
            openUsingBrowser(context, url);
        } else if (Check.isZhihuClientInstalled()) {
            openUsingZhihuClient(context, url);
        } else {
            openUsingBrowser(context, url);
        }
    }

    private void openUsingBrowser(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        if (Check.isIntentSafe(browserIntent)) {
            context.startActivity(browserIntent);
        } else {
            Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
        }
    }

    private void openUsingZhihuClient(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.setPackage(Constants.Information.ZHIHU_PACKAGE_ID);
        context.startActivity(browserIntent);
    }

    private void shareQuestion(Context context, String questionTitle, String questionUrl) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        //noinspection deprecation
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        share.putExtra(Intent.EXTRA_TEXT,
                questionTitle + " " + questionUrl + Constants.Strings.SHARE_FROM_ZHIHU);
        context.startActivity(Intent.createChooser(share, context.getString(R.string.share_to)));
    }

    private String[] getQuestionTitlesAsStringArray(ZhihuDailyPurify.News news) {
        return news.getQuestionsList().stream().map(ZhihuDailyPurify.Question::getTitle).toArray(String[]::new);
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView newsImage;
        TextView questionTitle;
        TextView dailyTitle;
        ImageView overflow;

        private ClickResponseListener mClickResponseListener;

        CardViewHolder(View v, ClickResponseListener clickResponseListener) {
            super(v);

            this.mClickResponseListener = clickResponseListener;

            newsImage = v.findViewById(R.id.thumbnail_image);
            questionTitle = v.findViewById(R.id.question_title);
            dailyTitle = v.findViewById(R.id.daily_title);
            overflow = v.findViewById(R.id.card_share_overflow);

            v.setOnClickListener(this);
            overflow.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == overflow) {
                mClickResponseListener.onOverflowClick(v, getAdapterPosition());
            } else {
                mClickResponseListener.onWholeClick(getAdapterPosition());
            }
        }

        public interface ClickResponseListener {
            void onWholeClick(int position);

            void onOverflowClick(View v, int position);
        }
    }

    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {
        static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }
}
