package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.CommentViewHolder;
import awais.instagrabber.interfaces.MentionClickListener;
import awais.instagrabber.models.CommentModel;
import awais.instagrabber.models.ProfileModel;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.Utils;

public final class CommentsAdapter extends RecyclerView.Adapter<CommentViewHolder> implements Filterable {
    private final boolean isParent;
    private final Filter filter = new Filter() {
        @NonNull
        @Override
        protected FilterResults performFiltering(final CharSequence filter) {
            final FilterResults results = new FilterResults();
            results.values = commentModels;

            final int commentsLen = commentModels == null ? 0 : commentModels.length;
            if (commentModels != null && commentsLen > 0 && !Utils.isEmpty(filter)) {
                final String query = filter.toString().toLowerCase();
                final ArrayList<CommentModel> filterList = new ArrayList<>(commentsLen);

                for (final CommentModel commentModel : commentModels) {
                    final String commentText = commentModel.getText().toString().toLowerCase();

                    if (commentText.contains(query)) filterList.add(commentModel);
                    else {
                        final CommentModel[] childCommentModels = commentModel.getChildCommentModels();
                        if (childCommentModels != null) {
                            for (final CommentModel childCommentModel : childCommentModels) {
                                final String childCommentText = childCommentModel.getText().toString().toLowerCase();
                                if (childCommentText.contains(query)) filterList.add(commentModel);
                            }
                        }
                    }
                }
                filterList.trimToSize();
                results.values = filterList.toArray(new CommentModel[0]);
            }

            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, @NonNull final FilterResults results) {
            if (results.values instanceof CommentModel[]) {
                filteredCommentModels = (CommentModel[]) results.values;
                notifyDataSetChanged();
            }
        }
    };
    private final View.OnClickListener onClickListener;
    private final MentionClickListener mentionClickListener;
    private final CommentModel[] commentModels;
    private final String[] quantityStrings = new String[2];
    private LayoutInflater layoutInflater;
    private CommentModel[] filteredCommentModels;

    public CommentsAdapter(final CommentModel[] commentModels, final boolean isParent, final View.OnClickListener onClickListener,
                           final MentionClickListener mentionClickListener) {
        this.commentModels = this.filteredCommentModels = commentModels;
        this.isParent = isParent;
        this.onClickListener = onClickListener;
        this.mentionClickListener = mentionClickListener;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int type) {
        final Context context = parent.getContext();
        if (quantityStrings[0] == null) quantityStrings[0] = context.getString(R.string.single_like);
        if (quantityStrings[1] == null) quantityStrings[1] = context.getString(R.string.multiple_likes);
        if (layoutInflater == null) layoutInflater = LayoutInflater.from(context);
        return new CommentViewHolder(layoutInflater.inflate(
                isParent ? R.layout.item_comment       // parent
                        : R.layout.item_comment_small, // child
                parent, false), onClickListener, mentionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentViewHolder holder, final int position) {
        final CommentModel commentModel = filteredCommentModels[position];
        if (commentModel != null) {
            holder.setCommentModel(commentModel);

            holder.setCommment(commentModel.getText());
            holder.setDate(commentModel.getDateTime());
            holder.setLiked(commentModel.getLiked());

            final long likes = commentModel.getLikes();
            holder.setLikes(String.format(LocaleUtils.getCurrentLocale(), "%d %s", likes, quantityStrings[likes == 1 ? 0 : 1]));

            final ProfileModel profileModel = commentModel.getProfileModel();
            if (profileModel != null) {
                holder.setUsername(profileModel.getUsername());

                Glide.with(layoutInflater.getContext())
                        .applyDefaultRequestOptions(new RequestOptions().skipMemoryCache(true))
                        .load(profileModel.getSdProfilePic()).into(holder.getProfilePicView());
            }

            if (holder.isParent()) {
                final CommentModel[] childCommentModels = commentModel.getChildCommentModels();
                if (childCommentModels != null && childCommentModels.length > 0)
                    holder.setChildAdapter(new CommentsAdapter(childCommentModels, false, onClickListener, mentionClickListener));
                else holder.hideChildComments();
            }
        }
    }

    @Override
    public int getItemCount() {
        return filteredCommentModels == null ? 0 : filteredCommentModels.length;
    }
}