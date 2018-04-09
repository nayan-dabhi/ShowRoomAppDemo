package com.ramotion.showroom.examples.garlandview.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ramotion.garlandview.TailLayoutManager;
import com.ramotion.garlandview.TailRecyclerView;
import com.ramotion.garlandview.TailSnapHelper;
import com.ramotion.garlandview.header.HeaderTransformer;
import com.ramotion.showroom.R;
import com.ramotion.showroom.examples.garlandview.RxFaker;
import com.ramotion.showroom.examples.garlandview.details.GarlandViewDetailsActivity;
import com.ramotion.showroom.examples.garlandview.main.inner.InnerData;
import com.ramotion.showroom.examples.garlandview.main.inner.InnerItem;
import com.ramotion.showroom.examples.garlandview.main.outer.OuterAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import io.bloco.faker.Faker;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class GarlandViewMainActivity extends AppCompatActivity implements RxFaker.FakerReadyListener {

    private final static int OUTER_COUNT = 10;
    private final static int INNER_COUNT = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gv_activity_main);

        RxFaker.addListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onFakerReady(final Faker faker) {
        Single.create(new SingleOnSubscribe<List<List<InnerData>>>() {
            @Override
            public void subscribe(SingleEmitter<List<List<InnerData>>> e) throws Exception {
                final List<List<InnerData>> outerData = new ArrayList<>();
                for (int i = 0; i < OUTER_COUNT && !e.isDisposed(); i++) {
                    final List<InnerData> innerData = new ArrayList<>();
                    for (int j = 0; j < INNER_COUNT && !e.isDisposed(); j++) {
                        innerData.add(createInnerData(faker));
                    }
                    outerData.add(innerData);
                }

                if (!e.isDisposed()) {
                    e.onSuccess(outerData);
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<List<List<InnerData>>>() {
            @Override
            public void accept(List<List<InnerData>> outerData) throws Exception {
                initRecyclerView(outerData);
            }
        });
    }

    private void initRecyclerView(List<List<InnerData>> data) {
        findViewById(R.id.progressBar).setVisibility(View.GONE);

        final TailRecyclerView rv = (TailRecyclerView) findViewById(R.id.recycler_view);
        ((TailLayoutManager) rv.getLayoutManager()).setPageTransformer(new HeaderTransformer());
        rv.setAdapter(new OuterAdapter(data));

        new TailSnapHelper().attachToRecyclerView(rv);
    }

    private InnerData createInnerData(Faker faker) {
        return new InnerData(
                faker.book.title(),
                faker.name.name(),
                faker.address.city() + ", " + faker.address.stateAbbr(),
                faker.avatar.image(faker.internet.email(), "150x150", "jpg", "set1", "bg1"),
                faker.number.between(20, 50)
        );
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnInnerItemClick(InnerItem item) {
        final InnerData itemData = item.getItemData();
        if (itemData == null) {
            return;
        }

        GarlandViewDetailsActivity.start(this,
                item.getItemData().name, item.mAddress.getText().toString(),
                item.getItemData().avatarUrl, item.itemView, item.mAvatarBorder);
    }

}
