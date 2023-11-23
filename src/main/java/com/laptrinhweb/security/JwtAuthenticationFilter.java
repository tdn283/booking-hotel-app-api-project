package com.laptrinhweb.security;

import com.laptrinhweb.entity.JwtToken;
import com.laptrinhweb.entity.User;
import com.laptrinhweb.repository.JwtTokenRepository;
import com.laptrinhweb.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    JwtService jwtService;
    @Autowired
    MyUserDetailService service;
    @Autowired
    JwtTokenRepository jwtTokenRepository;

    @Override
    protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull  HttpServletResponse response,
          @NonNull  FilterChain filterChain
    ) throws ServletException, IOException {
        String urlPermitAll=request.getServletPath();
        if((urlPermitAll.contains("/api/v1/auth/") && !urlPermitAll.contains("change") )|| urlPermitAll.contains("swagger-ui") ){
            filterChain.doFilter(request,response);
            return;
        }
        String authHeader=request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer")){
            filterChain.doFilter(request,response);
            return;
        }
        String jwtToken=authHeader.substring(7);
        String username=jwtService.extractUsername(jwtToken);
        if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
            UserDetails user=null;
            JwtToken token=null;
            try {
                user = service.loadUserByUsername(username);
                token=jwtTokenRepository.findByToken(jwtToken);
            }catch (Exception e){
                e.printStackTrace();
            }



            if(jwtService.validateToken(jwtToken, user)
                    && token.isEnabled() && token.isNonExpired() ){
                UsernamePasswordAuthenticationToken authenticationToken=
                        new UsernamePasswordAuthenticationToken(
                                user,
                                user,
                                user.getAuthorities()
                        );
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);


            }

        }
        filterChain.doFilter(request,response);

    }
}
