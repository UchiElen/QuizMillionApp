
package com.dam.quizmillionapp.interfaces;

import com.dam.quizmillionapp.models.MemberListItem;
import java.util.List;

public interface LoadMembersCallback {
    void onMembersLoaded(List<MemberListItem> members);
    void onError(String errorMessage);
}