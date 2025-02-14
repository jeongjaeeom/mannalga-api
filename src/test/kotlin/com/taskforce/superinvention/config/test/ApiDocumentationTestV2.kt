package com.taskforce.superinvention.config.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.taskforce.superinvention.app.domain.user.*
import com.taskforce.superinvention.common.config.argument.resolver.auth.AuthorizeArgumentResolver
import com.taskforce.superinvention.common.config.security.JwtTokenProvider
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureRestDocs
abstract class ApiDocumentationTestV2: BaseTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var authorizeArgumentResolver: AuthorizeArgumentResolver

    @MockkBean
    lateinit var userRepository: UserRepository

    @MockkBean(relaxed = true)
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    lateinit var userDetailsProvider: UserDetailsProvider

    // @AuthUser - ArgumentResolver 모킹
    fun initMockAuthUser(user: User) {
        every { authorizeArgumentResolver.resolveArgument(any(), any(), any(), any()) }.returns(user)
        every { authorizeArgumentResolver.supportsParameter(any())}.returns(true)
    }
}